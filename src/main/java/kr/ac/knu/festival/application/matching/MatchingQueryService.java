package kr.ac.knu.festival.application.matching;

import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
import kr.ac.knu.festival.domain.matching.entity.MatchingServiceState;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.domain.matching.repository.MatchingServiceStateRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingAuthRequest;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingResultResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingStatusResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.UnmatchedParticipantResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.UnmatchedParticipantsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingQueryService {

    private final MatchingParticipantRepository matchingParticipantRepository;
    private final MatchingServiceStateRepository matchingServiceStateRepository;
    private final PasswordEncoder passwordEncoder;
    private final MatchingScheduleProperties matchingScheduleProperties;
    private final MatchingRealtimeCache matchingRealtimeCache;
    private final MatchingRateLimiter matchingRateLimiter;

    public MatchingResultResponse getResult(MatchingAuthRequest request, String clientIp) {
        matchingRateLimiter.validateAllowed(clientIp);
        MatchingParticipant participant = matchingParticipantRepository.findById(normalizeInstagramId(request.instagramId()))
                .orElseThrow(() -> {
                    matchingRateLimiter.recordFailure(clientIp);
                    return new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND);
                });
        if (!passwordEncoder.matches(request.password(), participant.getPassword())) {
            matchingRateLimiter.recordFailure(clientIp);
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED_USER);
        }
        matchingRateLimiter.reset(clientIp);
        if (!matchingScheduleProperties.isResultOpen()) {
            return MatchingResultResponse.hidden(participant);
        }
        matchingRealtimeCache.cacheParticipantResult(participant);
        return MatchingResultResponse.fromEntity(participant);
    }

    public MatchingStatusResponse getStatus() {
        Map<Object, Object> cachedStatus = matchingRealtimeCache.getStatus();
        if (!cachedStatus.isEmpty()) {
            Map<Object, Object> cachedCounts = matchingRealtimeCache.getParticipantCounts();
            return MatchingStatusResponse.of(
                    matchingServiceStateRepository.findById(MatchingServiceState.SINGLETON_ID)
                            .orElse(MatchingServiceState.defaultOpen()),
                    matchingScheduleProperties.isRegistrationOpen(),
                    matchingScheduleProperties.isResultOpen(),
                    valueOf(cachedStatus, "registrationDeadline"),
                    valueOf(cachedStatus, "resultOpenAt"),
                    longValueOf(cachedCounts, "pending"),
                    longValueOf(cachedCounts, "matched"),
                    longValueOf(cachedCounts, "unmatched")
            );
        }

        MatchingServiceState state = matchingServiceStateRepository.findById(MatchingServiceState.SINGLETON_ID)
                .orElse(MatchingServiceState.defaultOpen());
        long pendingCount = matchingParticipantRepository.countByStatus(MatchingParticipantStatus.PENDING);
        long matchedCount = matchingParticipantRepository.countByStatus(MatchingParticipantStatus.MATCHED);
        long unmatchedCount = matchingParticipantRepository.countByStatus(MatchingParticipantStatus.UNMATCHED);
        matchingRealtimeCache.cacheStatus(state, matchingScheduleProperties, pendingCount, matchedCount, unmatchedCount);
        return MatchingStatusResponse.of(
                state,
                state.getStatus().name().equals("OPEN") && matchingScheduleProperties.isRegistrationOpen(),
                matchingScheduleProperties.isResultOpen(),
                matchingScheduleProperties.registrationDeadline().toString(),
                matchingScheduleProperties.resultOpenAt().toString(),
                pendingCount,
                matchedCount,
                unmatchedCount
        );
    }

    public UnmatchedParticipantsResponse getUnmatchedParticipants() {
        if (!matchingScheduleProperties.isResultOpen()) {
            return UnmatchedParticipantsResponse.hidden(matchingScheduleProperties.resultOpenAt().toString());
        }

        List<UnmatchedParticipantResponse> participants = matchingParticipantRepository.findAllByStatus(MatchingParticipantStatus.UNMATCHED).stream()
                .map(UnmatchedParticipantResponse::fromEntity)
                .toList();
        return UnmatchedParticipantsResponse.open(matchingScheduleProperties.resultOpenAt().toString(), participants);
    }

    private String normalizeInstagramId(String instagramId) {
        return instagramId.trim().replaceFirst("^@", "").toLowerCase();
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
