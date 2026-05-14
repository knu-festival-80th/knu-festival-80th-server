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
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingAuthRequest;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingApplicantsCountResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingParticipantAdminResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingParticipantsAdminResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingResultResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingStatusResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.UnmatchedParticipantResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.UnmatchedParticipantsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingQueryService {

    private final MatchingParticipantRepository matchingParticipantRepository;
    private final MatchingServiceStateRepository matchingServiceStateRepository;
    private final PhoneLookupHasher phoneLookupHasher;
    private final PhoneNumberEncryptor phoneNumberEncryptor;
    private final MatchingScheduleProperties matchingScheduleProperties;
    private final MatchingRealtimeCache matchingRealtimeCache;
    private final MatchingRateLimiter matchingRateLimiter;

    public MatchingResultResponse getResult(MatchingAuthRequest request, String clientIp) {
        matchingRateLimiter.validateAllowed(clientIp);

        Optional<LocalDate> resultDayOpt = matchingScheduleProperties.currentResultDay();
        Optional<LocalDate> registrationDayOpt = matchingScheduleProperties.currentRegistrationDay();
        LocalDate lookupDay = resultDayOpt.or(() -> registrationDayOpt).orElse(null);
        if (lookupDay == null) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND);
        }

        String instagramId = MatchingParticipant.normalizeInstagramId(request.instagramId());
        MatchingParticipant participant = matchingParticipantRepository
                .findByInstagramIdAndFestivalDay(instagramId, lookupDay)
                .orElseThrow(() -> {
                    matchingRateLimiter.recordFailure(clientIp);
                    return new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND);
                });
        String expectedHash = phoneLookupHasher.hash(normalizePhone(request.phoneNumber()));
        if (!expectedHash.equals(participant.getPhoneLookupHash())) {
            matchingRateLimiter.recordFailure(clientIp);
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED_USER);
        }
        matchingRateLimiter.reset(clientIp);
        if (resultDayOpt.isEmpty()) {
            return MatchingResultResponse.hidden(participant, matchingScheduleProperties.upcomingResultOpenIso());
        }
        matchingRealtimeCache.cacheParticipantResult(participant);
        return MatchingResultResponse.fromEntity(participant);
    }

    public MatchingStatusResponse getStatus() {
        Map<Object, Object> cachedStatus = matchingRealtimeCache.getStatus();
        if (!cachedStatus.isEmpty()) {
            Map<Object, Object> cachedCounts = matchingRealtimeCache.getParticipantCounts();
            MatchingOperationStatus status = MatchingOperationStatus.valueOf(valueOf(cachedStatus, "status"));
            return MatchingStatusResponse.ofCached(
                    status,
                    status == MatchingOperationStatus.OPEN && matchingScheduleProperties.isRegistrationOpen(),
                    matchingScheduleProperties.isResultOpen(),
                    valueOf(cachedStatus, "registrationDeadline"),
                    valueOf(cachedStatus, "resultOpenAt"),
                    matchingScheduleProperties.festivalDays(),
                    longValueOf(cachedCounts, "pending"),
                    longValueOf(cachedCounts, "matched"),
                    longValueOf(cachedCounts, "unmatched"),
                    longValueOf(cachedCounts, "malePending"),
                    longValueOf(cachedCounts, "femalePending")
            );
        }
        return computeStatusFromDb();
    }

    public MatchingApplicantsCountResponse getApplicantsCount() {
        LocalDate day = currentDayForCounts();
        if (day == null) {
            return MatchingApplicantsCountResponse.empty();
        }
        long male = matchingParticipantRepository.countByFestivalDayAndStatusAndGender(
                day, MatchingParticipantStatus.PENDING, MatchingGender.MALE);
        long female = matchingParticipantRepository.countByFestivalDayAndStatusAndGender(
                day, MatchingParticipantStatus.PENDING, MatchingGender.FEMALE);
        return new MatchingApplicantsCountResponse(day, male, female, male + female);
    }

    public MatchingParticipantsAdminResponse listParticipantsForAdmin(
            LocalDate festivalDay,
            MatchingParticipantStatus status,
            MatchingGender gender,
            String search
    ) {
        LocalDate targetDay = festivalDay != null ? festivalDay : currentDayForCounts();
        if (targetDay == null) {
            targetDay = matchingScheduleProperties.festivalDays().isEmpty()
                    ? null
                    : matchingScheduleProperties.festivalDays().get(matchingScheduleProperties.festivalDays().size() - 1);
        }
        if (targetDay == null) {
            return MatchingParticipantsAdminResponse.of(null, List.of());
        }
        String trimmedSearch = (search == null || search.isBlank()) ? null : search.trim().toLowerCase();
        List<MatchingParticipantAdminResponse> rows = matchingParticipantRepository
                .searchForAdmin(targetDay, status, gender, trimmedSearch)
                .stream()
                .map(p -> MatchingParticipantAdminResponse.fromEntity(p, maskedPhone(p.getPhoneEncrypted())))
                .toList();
        return MatchingParticipantsAdminResponse.of(targetDay, rows);
    }

    private String maskedPhone(String encryptedPhone) {
        if (encryptedPhone == null) {
            return "";
        }
        String decrypted = phoneNumberEncryptor.decrypt(encryptedPhone);
        if (decrypted == null) {
            return "";
        }
        String digits = decrypted.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "***-****-****";
        }
        return "***-****-" + digits.substring(digits.length() - 4);
    }

    public UnmatchedParticipantsResponse getUnmatchedParticipants() {
        Optional<LocalDate> resultDayOpt = matchingScheduleProperties.currentResultDay();
        if (resultDayOpt.isEmpty()) {
            return UnmatchedParticipantsResponse.hidden(matchingScheduleProperties.upcomingResultOpenIso());
        }
        List<UnmatchedParticipantResponse> participants = matchingParticipantRepository
                .findAllByFestivalDayAndStatus(resultDayOpt.get(), MatchingParticipantStatus.UNMATCHED).stream()
                .map(UnmatchedParticipantResponse::fromEntity)
                .toList();
        return UnmatchedParticipantsResponse.open(matchingScheduleProperties.upcomingResultOpenIso(), participants);
    }

    private MatchingStatusResponse computeStatusFromDb() {
        MatchingServiceState state = matchingServiceStateRepository.findById(MatchingServiceState.SINGLETON_ID)
                .orElse(MatchingServiceState.defaultOpen());
        LocalDate day = currentDayForCounts();
        long pending = day == null ? 0 : matchingParticipantRepository.countByFestivalDayAndStatus(day, MatchingParticipantStatus.PENDING);
        long matched = day == null ? 0 : matchingParticipantRepository.countByFestivalDayAndStatus(day, MatchingParticipantStatus.MATCHED);
        long unmatched = day == null ? 0 : matchingParticipantRepository.countByFestivalDayAndStatus(day, MatchingParticipantStatus.UNMATCHED);
        long malePending = day == null ? 0 : matchingParticipantRepository.countByFestivalDayAndStatusAndGender(day, MatchingParticipantStatus.PENDING, MatchingGender.MALE);
        long femalePending = day == null ? 0 : matchingParticipantRepository.countByFestivalDayAndStatusAndGender(day, MatchingParticipantStatus.PENDING, MatchingGender.FEMALE);
        return MatchingStatusResponse.of(
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
    }

    // 카운트 표시는 신청창에선 오늘, 결과창에선 그 결과의 원본 일자를 기준으로 한다.
    private LocalDate currentDayForCounts() {
        return matchingScheduleProperties.currentRegistrationDay()
                .or(matchingScheduleProperties::currentResultDay)
                .orElse(null);
    }

    private String normalizePhone(String phoneNumber) {
        return phoneNumber == null ? null : phoneNumber.replaceAll("\\D", "");
    }

    private String valueOf(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : value.toString();
    }

    private long longValueOf(Map<Object, Object> values, String key) {
        String value = valueOf(values, key);
        if (value.isBlank()) {
            return 0L;
        }
        return Long.parseLong(value);
    }
}
