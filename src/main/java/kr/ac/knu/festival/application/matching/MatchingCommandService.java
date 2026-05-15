package kr.ac.knu.festival.application.matching;

import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingOperationStatus;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
import kr.ac.knu.festival.domain.matching.entity.MatchingServiceState;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.domain.matching.repository.MatchingServiceStateRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.infra.security.PhoneLookupHasher;
import kr.ac.knu.festival.infra.security.PhoneNumberEncryptor;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingCreateRequest;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingStatusUpdateRequest;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingJobResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingRegisterResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchingCommandService {

    private final MatchingParticipantRepository matchingParticipantRepository;
    private final MatchingServiceStateRepository matchingServiceStateRepository;
    private final PhoneLookupHasher phoneLookupHasher;
    private final PhoneNumberEncryptor phoneNumberEncryptor;
    private final MatchingScheduleProperties matchingScheduleProperties;
    private final MatchingRealtimeCache matchingRealtimeCache;

    public MatchingRegisterResponse register(MatchingCreateRequest request) {
        MatchingServiceState state = getOrCreateState();
        LocalDate festivalDay = matchingScheduleProperties.currentRegistrationDay()
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MATCHING_REGISTRATION_CLOSED));
        if (state.getStatus() != MatchingOperationStatus.OPEN) {
            throw new BusinessException(BusinessErrorCode.MATCHING_REGISTRATION_CLOSED);
        }

        String instagramId = MatchingParticipant.normalizeInstagramId(request.instagramId());
        if (matchingParticipantRepository.existsByInstagramIdAndFestivalDay(instagramId, festivalDay)) {
            throw new BusinessException(BusinessErrorCode.MATCHING_DUPLICATE_REGISTRATION);
        }

        String normalizedPhone = normalizePhone(request.phoneNumber());
        String lookupHash = phoneLookupHasher.hash(normalizedPhone);
        if (matchingParticipantRepository.existsByPhoneLookupHashAndFestivalDay(lookupHash, festivalDay)) {
            throw new BusinessException(BusinessErrorCode.MATCHING_DUPLICATE_PHONE);
        }
        MatchingParticipant participant = MatchingParticipant.create(
                instagramId,
                festivalDay,
                request.gender(),
                lookupHash,
                phoneNumberEncryptor.encrypt(normalizedPhone)
        );
        MatchingParticipant saved = matchingParticipantRepository.save(participant);
        matchingRealtimeCache.cacheParticipantResult(saved);
        refreshRealtimeStatus(state, festivalDay);
        return MatchingRegisterResponse.fromEntity(
                saved,
                matchingScheduleProperties.upcomingRegistrationDeadlineIso(),
                matchingScheduleProperties.upcomingResultOpenIso()
        );
    }

    public MatchingJobResponse runMatchingJob() {
        LocalDate targetDay = matchingScheduleProperties.dayPendingMatching()
                .or(matchingScheduleProperties::currentResultDay)
                .or(matchingScheduleProperties::currentRegistrationDay)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MATCHING_REGISTRATION_CLOSED));
        return runMatchingJobFor(targetDay);
    }

    public MatchingJobResponse runMatchingJobOn(LocalDate festivalDay) {
        if (!matchingScheduleProperties.festivalDays().contains(festivalDay)) {
            throw new BusinessException(BusinessErrorCode.INVALID_INPUT_VALUE);
        }
        return runMatchingJobFor(festivalDay);
    }

    public MatchingStatusResponse deleteParticipant(Long participantId) {
        MatchingParticipant participant = matchingParticipantRepository.findById(participantId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND));
        LocalDate day = participant.getFestivalDay();
        String instagramId = participant.getInstagramId();
        matchingParticipantRepository.delete(participant);
        matchingRealtimeCache.evictParticipantResult(day, instagramId);
        return refreshRealtimeStatus(getOrCreateState(), day);
    }

    public MatchingStatusResponse resetParticipant(Long participantId) {
        MatchingParticipant participant = matchingParticipantRepository.findById(participantId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND));
        participant.resetToPending();
        // PENDING 으로 되돌릴 때는 이전에 캐시된 MATCHED/UNMATCHED 결과를 비운 뒤 새 PENDING 상태로 덮어쓴다.
        matchingRealtimeCache.evictParticipantResult(participant.getFestivalDay(), participant.getInstagramId());
        matchingRealtimeCache.cacheParticipantResult(participant);
        return refreshRealtimeStatus(getOrCreateState(), participant.getFestivalDay());
    }

    public MatchingJobResponse runMatchingJobFor(LocalDate festivalDay) {
        List<MatchingParticipant> males = matchingParticipantRepository.findAllByDayAndStatusAndGenderForUpdate(
                festivalDay, MatchingParticipantStatus.PENDING, MatchingGender.MALE);
        List<MatchingParticipant> females = matchingParticipantRepository.findAllByDayAndStatusAndGenderForUpdate(
                festivalDay, MatchingParticipantStatus.PENDING, MatchingGender.FEMALE);

        MatchingPairing.Result result = new MatchingPairing(new Random()).pair(males, females);

        for (MatchingPairing.MatchedPair pair : result.matched()) {
            pair.picker().matchWith(pair.picked().getInstagramId());
            matchingRealtimeCache.cacheParticipantResult(pair.picker());
        }
        for (MatchingParticipant participant : result.unmatched()) {
            participant.markUnmatched();
            matchingRealtimeCache.cacheParticipantResult(participant);
        }

        refreshRealtimeStatus(getOrCreateState(), festivalDay);
        int pickerCount = result.matched().size();
        // picker 행은 남녀 각각 잡으므로 실제 사람 수는 pickerCount/2 (단, N=1 fallback 도 동일하게 2건).
        int matchedPersonCount = pickerCount / 2;
        return new MatchingJobResponse(matchedPersonCount, result.unmatched().size());
    }

    public MatchingStatusResponse updateStatus(MatchingStatusUpdateRequest request) {
        MatchingServiceState state = getOrCreateState();
        state.changeStatus(request.status());
        LocalDate day = matchingScheduleProperties.currentRegistrationDay()
                .or(matchingScheduleProperties::currentResultDay)
                .orElse(null);
        return refreshRealtimeStatus(state, day);
    }

    private MatchingServiceState getOrCreateState() {
        return matchingServiceStateRepository.findById(MatchingServiceState.SINGLETON_ID)
                .orElseGet(() -> matchingServiceStateRepository.save(MatchingServiceState.defaultOpen()));
    }

    private String normalizePhone(String phoneNumber) {
        return phoneNumber == null ? null : phoneNumber.replaceAll("\\D", "");
    }

    private MatchingStatusResponse refreshRealtimeStatus(MatchingServiceState state, LocalDate day) {
        // (status, gender) → count 를 단일 GROUP BY 쿼리로 한 번에 조회한다.
        Map<MatchingParticipantStatus, Map<MatchingGender, Long>> counts = aggregateCounts(day);
        long pending = totalForStatus(counts, MatchingParticipantStatus.PENDING);
        long matched = totalForStatus(counts, MatchingParticipantStatus.MATCHED);
        long unmatched = totalForStatus(counts, MatchingParticipantStatus.UNMATCHED);
        long malePending = countFor(counts, MatchingParticipantStatus.PENDING, MatchingGender.MALE);
        long femalePending = countFor(counts, MatchingParticipantStatus.PENDING, MatchingGender.FEMALE);

        MatchingStatusResponse response = MatchingStatusResponse.of(
                state,
                state.getStatus() == MatchingOperationStatus.OPEN && matchingScheduleProperties.isRegistrationOpen(),
                matchingScheduleProperties.isResultOpen(),
                matchingScheduleProperties.upcomingRegistrationDeadlineIso(),
                matchingScheduleProperties.upcomingResultOpenIso(),
                matchingScheduleProperties.festivalDays(),
                pending,
                matched,
                unmatched,
                malePending,
                femalePending
        );
        matchingRealtimeCache.cacheStatus(state, matchingScheduleProperties, pending, matched, unmatched, malePending, femalePending);
        return response;
    }

    private Map<MatchingParticipantStatus, Map<MatchingGender, Long>> aggregateCounts(LocalDate day) {
        Map<MatchingParticipantStatus, Map<MatchingGender, Long>> counts = new EnumMap<>(MatchingParticipantStatus.class);
        if (day == null) {
            return counts;
        }
        for (Object[] row : matchingParticipantRepository.countByDayGroupByStatusGender(day)) {
            MatchingParticipantStatus status = (MatchingParticipantStatus) row[0];
            MatchingGender gender = (MatchingGender) row[1];
            long count = ((Number) row[2]).longValue();
            counts.computeIfAbsent(status, s -> new EnumMap<>(MatchingGender.class)).put(gender, count);
        }
        return counts;
    }

    private long totalForStatus(Map<MatchingParticipantStatus, Map<MatchingGender, Long>> counts, MatchingParticipantStatus status) {
        Map<MatchingGender, Long> byGender = counts.get(status);
        if (byGender == null) {
            return 0L;
        }
        return byGender.values().stream().mapToLong(Long::longValue).sum();
    }

    private long countFor(Map<MatchingParticipantStatus, Map<MatchingGender, Long>> counts, MatchingParticipantStatus status, MatchingGender gender) {
        Map<MatchingGender, Long> byGender = counts.get(status);
        if (byGender == null) {
            return 0L;
        }
        return byGender.getOrDefault(gender, 0L);
    }
}
