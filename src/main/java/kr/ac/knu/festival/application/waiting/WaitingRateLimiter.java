package kr.ac.knu.festival.application.waiting;

import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 단위 대기 등록 호출 빈도를 제한한다.
 * Redis 기반 카운터를 우선 사용하고, Redis 장애 시 in-memory fallback 으로 동작한다.
 *
 * NFR-SEC: 동일 IP 가 짧은 시간 안에 대기 등록을 반복 호출해 부스 락을 소비하는 행위를 차단한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingRateLimiter {

    private static final int MAX_REGISTRATIONS = 5;
    private static final Duration WINDOW = Duration.ofSeconds(10);
    private static final String KEY_PREFIX = "waiting:ratelimit:";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final Map<String, LocalAttempt> localAttempts = new ConcurrentHashMap<>();

    public void recordRegistration(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return;
        }
        long count = incrementAndGet(clientIp);
        if (count > MAX_REGISTRATIONS) {
            throw new BusinessException(BusinessErrorCode.WAITING_RATE_LIMITED);
        }
    }

    private long incrementAndGet(String clientIp) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            try {
                Long count = redisTemplate.opsForValue().increment(redisKey(clientIp));
                if (count != null && count == 1L) {
                    redisTemplate.expire(redisKey(clientIp), WINDOW);
                }
                if (count != null) {
                    return count;
                }
            } catch (DataAccessException ex) {
                log.debug("Redis rate-limit increment failed, falling back to in-memory: {}", ex.getMessage());
                // fall through to local fallback
            }
        }
        return incrementLocal(clientIp);
    }

    private long incrementLocal(String clientIp) {
        LocalAttempt updated = localAttempts.compute(clientIp, (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new LocalAttempt(1, Instant.now().plus(WINDOW));
            }
            return new LocalAttempt(existing.count() + 1, existing.expiresAt());
        });
        return updated.count();
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
