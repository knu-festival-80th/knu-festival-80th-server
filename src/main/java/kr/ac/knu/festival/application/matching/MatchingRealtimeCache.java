package kr.ac.knu.festival.application.matching;

import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
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
            long unmatchedCount
    ) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            // 프론트가 상태 API를 자주 호출해도 DB 집계를 반복하지 않도록 현재 상태와 카운트를 Redis에 올려둔다.
            redisTemplate.opsForHash().putAll(STATUS_KEY, Map.of(
                    "status", state.getStatus().name(),
                    "messageKo", state.getMessageKo(),
                    "messageEn", state.getMessageEn(),
                    "registrationDeadline", schedule.registrationDeadline().toString(),
                    "resultOpenAt", schedule.resultOpenAt().toString(),
                    "registrationOpen", Boolean.toString(schedule.isRegistrationOpen()),
                    "resultOpen", Boolean.toString(schedule.isResultOpen())
            ));
            redisTemplate.opsForHash().putAll(PARTICIPANT_COUNT_KEY, Map.of(
                    "pending", Long.toString(pendingCount),
                    "matched", Long.toString(matchedCount),
                    "unmatched", Long.toString(unmatchedCount)
            ));
        } catch (DataAccessException ignored) {
            // Redis는 실시간 표시용 보조 저장소라 실패해도 DB 기반 신청/조회 흐름은 유지한다.
        }
    }

    public void cacheParticipantResult(MatchingParticipant participant) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForHash().putAll(RESULT_KEY_PREFIX + participant.getInstagramId(), Map.of(
                    "status", participant.getStatus().name(),
                    "matchedId", participant.getMatchedId() == null ? "" : participant.getMatchedId()
            ));
        } catch (DataAccessException ignored) {
            // Redis 캐시 실패는 DB 트랜잭션 성공 여부와 분리한다.
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

    public Map<Object, Object> getParticipantResult(String instagramId) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return Map.of();
        }
        try {
            return redisTemplate.opsForHash().entries(RESULT_KEY_PREFIX + instagramId);
        } catch (DataAccessException ignored) {
            return Map.of();
        }
    }
}
