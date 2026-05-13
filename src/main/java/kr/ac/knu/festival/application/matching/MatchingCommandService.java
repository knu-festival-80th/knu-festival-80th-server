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
import java.util.Collections;
import java.util.List;

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
        MatchingParticipant participant = MatchingParticipant.create(
                instagramId,
                festivalDay,
                request.gender(),
                phoneLookupHasher.hash(normalizedPhone),
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
        // 매칭 실행창(21~22시) → 결과창(22~익11시) 어디든 호출 가능. 데드존이면 그날 데이터를, 결과창 안이면 그 결과창 일자를 잡는다.
        LocalDate targetDay = matchingScheduleProperties.dayPendingMatching()
                .or(matchingScheduleProperties::currentResultDay)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MATCHING_REGISTRATION_CLOSED));
        return runMatchingJobFor(targetDay);
    }

    public MatchingJobResponse runMatchingJobFor(LocalDate festivalDay) {
        List<MatchingParticipant> males = matchingParticipantRepository.findAllByDayAndStatusAndGenderForUpdate(
                festivalDay, MatchingParticipantStatus.PENDING, MatchingGender.MALE);
        List<MatchingParticipant> females = matchingParticipantRepository.findAllByDayAndStatusAndGenderForUpdate(
                festivalDay, MatchingParticipantStatus.PENDING, MatchingGender.FEMALE);

        if (pauseWhenGenderImbalanced(males.size(), females.size(), festivalDay)) {
            return new MatchingJobResponse(0, males.size() + females.size());
        }

        Collections.shuffle(males);
        Collections.shuffle(females);

        int pairCount = Math.min(males.size(), females.size());
        for (int i = 0; i < pairCount; i++) {
            MatchingParticipant male = males.get(i);
            MatchingParticipant female = females.get(i);
            male.matchWith(female.getInstagramId());
            female.matchWith(male.getInstagramId());
            matchingRealtimeCache.cacheParticipantResult(male);
            matchingRealtimeCache.cacheParticipantResult(female);
        }

        for (int i = pairCount; i < males.size(); i++) {
            males.get(i).markUnmatched();
            matchingRealtimeCache.cacheParticipantResult(males.get(i));
        }
        for (int i = pairCount; i < females.size(); i++) {
            females.get(i).markUnmatched();
            matchingRealtimeCache.cacheParticipantResult(females.get(i));
        }

        refreshRealtimeStatus(getOrCreateState(), festivalDay);
        return new MatchingJobResponse(pairCount, males.size() + females.size() - pairCount * 2);
    }

    public MatchingStatusResponse updateStatus(MatchingStatusUpdateRequest request) {
        MatchingServiceState state = getOrCreateState();
        state.changeStatus(
                request.status(),
                defaultMessageKo(request.status(), request.messageKo()),
                defaultMessageEn(request.status(), request.messageEn())
        );
        LocalDate day = matchingScheduleProperties.currentRegistrationDay()
                .or(matchingScheduleProperties::currentResultDay)
                .orElse(null);
        return refreshRealtimeStatus(state, day);
    }

    private MatchingServiceState getOrCreateState() {
        return matchingServiceStateRepository.findById(MatchingServiceState.SINGLETON_ID)
                .orElseGet(() -> matchingServiceStateRepository.save(MatchingServiceState.defaultOpen()));
    }

    private boolean pauseWhenGenderImbalanced(int maleCount, int femaleCount, LocalDate day) {
        int total = maleCount + femaleCount;
        if (total == 0) {
            return false;
        }
        if ((double) Math.max(maleCount, femaleCount) / total >= 0.7) {
            MatchingServiceState state = getOrCreateState();
            state.changeStatus(
                    MatchingOperationStatus.PAUSED,
                    "성별 비율 불균형으로 매칭이 일시중단되었습니다.",
                    "Matching is paused because the gender ratio is imbalanced."
            );
            refreshRealtimeStatus(state, day);
            return true;
        }
        return false;
    }

    private String normalizePhone(String phoneNumber) {
        return phoneNumber == null ? null : phoneNumber.replaceAll("\\D", "");
    }

    private String defaultMessageKo(MatchingOperationStatus status, String message) {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return status == MatchingOperationStatus.OPEN ? "매칭 신청이 가능합니다." : "매칭 신청이 일시중단되었습니다.";
    }

    private String defaultMessageEn(MatchingOperationStatus status, String message) {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return status == MatchingOperationStatus.OPEN ? "Matching is open." : "Matching is paused.";
    }

    private MatchingStatusResponse refreshRealtimeStatus(MatchingServiceState state, LocalDate day) {
        long pending = day == null ? 0 : matchingParticipantRepository.countByFestivalDayAndStatus(day, MatchingParticipantStatus.PENDING);
        long matched = day == null ? 0 : matchingParticipantRepository.countByFestivalDayAndStatus(day, MatchingParticipantStatus.MATCHED);
        long unmatched = day == null ? 0 : matchingParticipantRepository.countByFestivalDayAndStatus(day, MatchingParticipantStatus.UNMATCHED);
        long malePending = day == null ? 0 : matchingParticipantRepository.countByFestivalDayAndStatusAndGender(day, MatchingParticipantStatus.PENDING, MatchingGender.MALE);
        long femalePending = day == null ? 0 : matchingParticipantRepository.countByFestivalDayAndStatusAndGender(day, MatchingParticipantStatus.PENDING, MatchingGender.FEMALE);

        MatchingStatusResponse response = MatchingStatusResponse.of(
                state,
                state.getStatus() == MatchingOperationStatus.OPEN && matchingScheduleProperties.isRegistrationOpen(),
                matchingScheduleProperties.isResultOpen(),
                matchingScheduleProperties.upcomingRegistrationDeadlineIso(),
                matchingScheduleProperties.upcomingResultOpenIso(),
                pending,
                matched,
                unmatched,
                malePending,
                femalePending
        );
        matchingRealtimeCache.cacheStatus(state, matchingScheduleProperties, pending, matched, unmatched, malePending, femalePending);
        return response;
    }
}
