package kr.ac.knu.festival.application.matching;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MatchingRateLimiter {

    private static final int MAX_FAILURE_COUNT = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "matching:rate-limit:result:";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final Map<String, LocalAttempt> localAttempts = new ConcurrentHashMap<>();

    public void validateAllowed(String clientIp) {
        if (getFailureCount(clientIp) >= MAX_FAILURE_COUNT) {
            throw new MatchingRateLimitException();
        }
    }

    public void recordFailure(String clientIp) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                Long count = redisTemplate.opsForValue().increment(redisKey(clientIp));
                if (count != null && count == 1L) {
                    redisTemplate.expire(redisKey(clientIp), WINDOW);
                }
                return;
            } catch (DataAccessException ignored) {
                // Redis가 내려가도 결과 조회 자체를 막지 않도록 로컬 카운터로 대체한다.
            }
        }
        recordLocalFailure(clientIp);
    }

    public void reset(String clientIp) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(redisKey(clientIp));
            } catch (DataAccessException ignored) {
                // Redis 삭제 실패 시에도 로컬 카운터는 정리한다.
            }
        }
        localAttempts.remove(clientIp);
    }

    private int getFailureCount(String clientIp) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                String value = redisTemplate.opsForValue().get(redisKey(clientIp));
                return value == null ? 0 : Integer.parseInt(value);
            } catch (DataAccessException | NumberFormatException ignored) {
                // Redis 조회 실패나 잘못된 값은 로컬 카운터 기준으로 판단한다.
            }
        }
        LocalAttempt attempt = localAttempts.get(clientIp);
        if (attempt == null || attempt.isExpired()) {
            localAttempts.remove(clientIp);
            return 0;
        }
        return attempt.count();
    }

    private void recordLocalFailure(String clientIp) {
        localAttempts.compute(clientIp, (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new LocalAttempt(1, Instant.now().plus(WINDOW));
            }
            return new LocalAttempt(existing.count() + 1, existing.expiresAt());
        });
    }

    private String redisKey(String clientIp) {
        return KEY_PREFIX + clientIp;
    }

    private record LocalAttempt(int count, Instant expiresAt) {
        boolean isExpired() {
            return !Instant.now().isBefore(expiresAt);
        }
    }
}
