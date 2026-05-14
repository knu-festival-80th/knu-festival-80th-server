package kr.ac.knu.festival.application.matching;

import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingServiceState;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MatchingRealtimeCache {

    private static final String STATUS_KEY = "matching:status";
    private static final String PARTICIPANT_COUNT_KEY = "matching:participant-count";
    private static final String RESULT_KEY_PREFIX = "matching:result:";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public void cacheStatus(
            MatchingServiceState state,
            MatchingScheduleProperties schedule,
            long pendingCount,
            long matchedCount,
            long unmatchedCount,
            long malePendingCount,
            long femalePendingCount
    ) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForHash().putAll(STATUS_KEY, Map.of(
                    "status", state.getStatus().name(),
                    "registrationDeadline", schedule.upcomingRegistrationDeadlineIso(),
                    "resultOpenAt", schedule.upcomingResultOpenIso(),
                    "registrationOpen", Boolean.toString(schedule.isRegistrationOpen()),
                    "resultOpen", Boolean.toString(schedule.isResultOpen())
            ));
            redisTemplate.opsForHash().putAll(PARTICIPANT_COUNT_KEY, Map.of(
                    "pending", Long.toString(pendingCount),
                    "matched", Long.toString(matchedCount),
                    "unmatched", Long.toString(unmatchedCount),
                    "malePending", Long.toString(malePendingCount),
                    "femalePending", Long.toString(femalePendingCount)
            ));
        } catch (DataAccessException ignored) {
            // Redis 는 보조 저장소라 실패해도 DB 흐름은 유지한다.
        }
    }

    public void cacheParticipantResult(MatchingParticipant participant) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForHash().putAll(cacheKey(participant.getInstagramId(), participant.getFestivalDay().toString()), Map.of(
                    "status", participant.getStatus().name(),
                    "matchedId", participant.getMatchedId() == null ? "" : participant.getMatchedId()
            ));
        } catch (DataAccessException ignored) {
        }
    }

    public Map<Object, Object> getStatus() {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return Map.of();
        }
        try {
            return redisTemplate.opsForHash().entries(STATUS_KEY);
        } catch (DataAccessException ignored) {
            return Map.of();
        }
    }

    public Map<Object, Object> getParticipantCounts() {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return Map.of();
        }
        try {
            return redisTemplate.opsForHash().entries(PARTICIPANT_COUNT_KEY);
        } catch (DataAccessException ignored) {
            return Map.of();
        }
    }

    public Map<Object, Object> getParticipantResult(String instagramId, String festivalDay) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return Map.of();
        }
        try {
            return redisTemplate.opsForHash().entries(cacheKey(instagramId, festivalDay));
        } catch (DataAccessException ignored) {
            return Map.of();
        }
    }

    private String cacheKey(String instagramId, String festivalDay) {
        return RESULT_KEY_PREFIX + festivalDay + ":" + instagramId;
    }
}
