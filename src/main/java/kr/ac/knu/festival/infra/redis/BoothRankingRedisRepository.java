package kr.ac.knu.festival.infra.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Repository
public class BoothRankingRedisRepository {

    private static final String BOOTH_LIKES_KEY = "booth:likes";
    private static final String BOOTH_WAITING_COUNT_KEY = "booth:waiting-count";
    private static final String BOOTH_LIKED_KEY_PREFIX = "booth:liked:";
    private static final String BOOTH_LIKE_DIRTY_KEY = "booth:like:dirty";

    // KEYS[1] = liked set, KEYS[2] = likes zset, KEYS[3] = dirty set
    // ARGV[1] = userHash, ARGV[2] = boothId
    // returns 1 when SADD was new (score actually incremented), 0 otherwise
    private static final RedisScript<Long> ADD_LIKE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('SADD', KEYS[1], ARGV[1]) == 1 then "
                    + "redis.call('ZINCRBY', KEYS[2], 1, ARGV[2]); "
                    + "redis.call('SADD', KEYS[3], ARGV[2]); "
                    + "return 1; "
                    + "else return 0; end",
            Long.class);

    // returns 1 when SREM removed a member (score actually decremented), 0 otherwise
    // also clamps to zero in the same round-trip
    private static final RedisScript<Long> REMOVE_LIKE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('SREM', KEYS[1], ARGV[1]) == 1 then "
                    + "local s = redis.call('ZINCRBY', KEYS[2], -1, ARGV[2]); "
                    + "if tonumber(s) < 0 then redis.call('ZADD', KEYS[2], 0, ARGV[2]); end; "
                    + "redis.call('SADD', KEYS[3], ARGV[2]); "
                    + "return 1; "
                    + "else return 0; end",
            Long.class);

    // Generic clamp-at-zero used by waiting-count decrement paths.
    private static final RedisScript<Long> CLAMP_ZERO_SCRIPT = new DefaultRedisScript<>(
            "local s = redis.call('ZSCORE', KEYS[1], ARGV[1]); "
                    + "if s and tonumber(s) < 0 then "
                    + "redis.call('ZADD', KEYS[1], 0, ARGV[1]); return 1; "
                    + "end; return 0;",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public BoothRankingRedisRepository(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public RedisChangeResult addLike(Long boothId, String anonymousIdHash) {
        try {
            if (redisTemplate == null) {
                return RedisChangeResult.unavailableResult();
            }
            Long changed = redisTemplate.execute(
                    ADD_LIKE_SCRIPT,
                    List.of(likedKey(boothId), BOOTH_LIKES_KEY, BOOTH_LIKE_DIRTY_KEY),
                    anonymousIdHash, boothId.toString());
            return (changed != null && changed > 0)
                    ? RedisChangeResult.changedResult()
                    : RedisChangeResult.unchangedResult();
        } catch (Exception e) {
            log.warn("Redis addLike failed. boothId={}", boothId, e);
            return RedisChangeResult.unavailableResult();
        }
    }

    public RedisChangeResult removeLike(Long boothId, String anonymousIdHash) {
        try {
            if (redisTemplate == null) {
                return RedisChangeResult.unavailableResult();
            }
            Long changed = redisTemplate.execute(
                    REMOVE_LIKE_SCRIPT,
                    List.of(likedKey(boothId), BOOTH_LIKES_KEY, BOOTH_LIKE_DIRTY_KEY),
                    anonymousIdHash, boothId.toString());
            return (changed != null && changed > 0)
                    ? RedisChangeResult.changedResult()
                    : RedisChangeResult.unchangedResult();
        } catch (Exception e) {
            log.warn("Redis removeLike failed. boothId={}", boothId, e);
            return RedisChangeResult.unavailableResult();
        }
    }

    public void incrementWaitingCount(Long boothId) {
        incrementScore(BOOTH_WAITING_COUNT_KEY, boothId, 1);
    }

    public void decrementWaitingCount(Long boothId) {
        incrementScore(BOOTH_WAITING_COUNT_KEY, boothId, -1);
        clampScoreAtZero(BOOTH_WAITING_COUNT_KEY, boothId);
    }

    public void decrementWaitingCount(Long boothId, long count) {
        if (count <= 0) {
            return;
        }
        incrementScore(BOOTH_WAITING_COUNT_KEY, boothId, -count);
        clampScoreAtZero(BOOTH_WAITING_COUNT_KEY, boothId);
    }

    public void setLikes(Map<Long, Integer> likeCounts) {
        replaceScores(BOOTH_LIKES_KEY, likeCounts);
    }

    public void setWaitingCounts(Map<Long, Long> waitingCounts) {
        Map<Long, Integer> values = new HashMap<>();
        waitingCounts.forEach((boothId, count) -> values.put(boothId, Math.toIntExact(count)));
        replaceScores(BOOTH_WAITING_COUNT_KEY, values);
    }

    /**
     * 신규 부스 생성 시 ZSET에 0 점수로 등록하기 위한 단일 엔트리 헬퍼.
     */
    public void registerBooth(Long boothId) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String member = boothId.toString();
            redisTemplate.opsForZSet().add(BOOTH_LIKES_KEY, member, 0);
            redisTemplate.opsForZSet().add(BOOTH_WAITING_COUNT_KEY, member, 0);
        } catch (Exception e) {
            log.warn("Redis registerBooth failed. boothId={}", boothId, e);
        }
    }

    /**
     * 부스 삭제 시 ZSET 멤버 및 좋아요 SET, dirty 표시까지 정리한다.
     */
    public void evictBooth(Long boothId) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String member = boothId.toString();
            redisTemplate.opsForZSet().remove(BOOTH_LIKES_KEY, member);
            redisTemplate.opsForZSet().remove(BOOTH_WAITING_COUNT_KEY, member);
            redisTemplate.delete(likedKey(boothId));
            redisTemplate.opsForSet().remove(BOOTH_LIKE_DIRTY_KEY, member);
        } catch (Exception e) {
            log.warn("Redis evictBooth failed. boothId={}", boothId, e);
        }
    }

    public Map<Long, Integer> getLikeCounts(Collection<Long> boothIds) {
        return getScores(BOOTH_LIKES_KEY, boothIds);
    }

    public Map<Long, Integer> getWaitingCounts(Collection<Long> boothIds) {
        return getScores(BOOTH_WAITING_COUNT_KEY, boothIds);
    }

    public Map<Long, Integer> getAllLikeCounts() {
        try {
            if (redisTemplate == null) {
                return Collections.emptyMap();
            }
            Set<ZSetOperations.TypedTuple<String>> tuples =
                    redisTemplate.opsForZSet().rangeByScoreWithScores(
                            BOOTH_LIKES_KEY,
                            Double.NEGATIVE_INFINITY,
                            Double.POSITIVE_INFINITY);
            if (tuples == null || tuples.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<Long, Integer> result = new HashMap<>(tuples.size());
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                String value = tuple.getValue();
                Double score = tuple.getScore();
                if (value == null || score == null) {
                    continue;
                }
                result.put(Long.valueOf(value), score.intValue());
            }
            return result;
        } catch (Exception e) {
            log.warn("Redis getAllLikeCounts failed", e);
            return Collections.emptyMap();
        }
    }

    /**
     * dirty set 에 있는 boothId 만 좋아요 카운트를 반환하고, 동시에 dirty 표시를 비운다.
     * 한 라운드트립에 SMEMBERS + DEL 을 묶기 위해 SPOP-반복 대신 단일 트랜잭션을 사용한다.
     */
    public Map<Long, Integer> drainDirtyLikeCounts() {
        try {
            if (redisTemplate == null) {
                return Collections.emptyMap();
            }
            Set<String> dirty = redisTemplate.opsForSet().members(BOOTH_LIKE_DIRTY_KEY);
            if (dirty == null || dirty.isEmpty()) {
                return Collections.emptyMap();
            }
            // 변경분만 score 조회 후 dirty set 제거. 추가 dirty 항목이 그 사이 들어올 수 있어
            // SREM 으로 정확히 처리한 boothId 만 제거한다.
            List<Long> boothIds = new ArrayList<>(dirty.size());
            for (String value : dirty) {
                try {
                    boothIds.add(Long.valueOf(value));
                } catch (NumberFormatException ignored) {
                    // 잘못된 멤버는 정리 대상이 아니라 그대로 둔다.
                }
            }
            Map<Long, Integer> scores = getScores(BOOTH_LIKES_KEY, boothIds);
            redisTemplate.opsForSet().remove(BOOTH_LIKE_DIRTY_KEY, dirty.toArray());
            return scores;
        } catch (Exception e) {
            log.warn("Redis drainDirtyLikeCounts failed", e);
            return Collections.emptyMap();
        }
    }

    /**
     * fallback 경로(DB 직접 가감)에서 호출. sync 가 옛 Redis 값으로 덮어쓰는 lost-update 를 막기 위해
     * 다음 sync 사이클에서 해당 boothId 를 스킵하도록 dirty 표시에서 제거한다.
     */
    public void unmarkLikeDirty(Long boothId) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForSet().remove(BOOTH_LIKE_DIRTY_KEY, boothId.toString());
        } catch (Exception e) {
            log.warn("Redis unmarkLikeDirty failed. boothId={}", boothId, e);
        }
    }

    public int getLikeCount(Long boothId, int fallback) {
        try {
            if (redisTemplate == null) {
                return fallback;
            }
            Double score = redisTemplate.opsForZSet().score(BOOTH_LIKES_KEY, boothId.toString());
            return score == null ? fallback : score.intValue();
        } catch (Exception e) {
            log.warn("Redis getLikeCount failed. boothId={}", boothId, e);
            return fallback;
        }
    }

    private void incrementScore(String key, Long boothId, long delta) {
        try {
            if (redisTemplate == null) {
                return;
            }
            redisTemplate.opsForZSet().incrementScore(key, boothId.toString(), delta);
        } catch (Exception e) {
            log.warn("Redis incrementScore failed. key={}, boothId={}, delta={}", key, boothId, delta, e);
        }
    }

    private void replaceScores(String key, Map<Long, Integer> values) {
        try {
            if (redisTemplate == null) {
                return;
            }
            redisTemplate.delete(key);
            if (values.isEmpty()) {
                return;
            }
            Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>(values.size());
            values.forEach((boothId, score) ->
                    tuples.add(ZSetOperations.TypedTuple.of(boothId.toString(), (double) score)));
            redisTemplate.opsForZSet().add(key, tuples);
        } catch (Exception e) {
            log.warn("Redis replaceScores failed. key={}", key, e);
        }
    }

    private Map<Long, Integer> getScores(String key, Collection<Long> boothIds) {
        try {
            if (redisTemplate == null || boothIds.isEmpty()) {
                return Collections.emptyMap();
            }
            // dedupe and stabilise order
            Set<Long> deduped = new LinkedHashSet<>(boothIds);
            List<Long> orderedIds = new ArrayList<>(deduped);
            byte[][] members = new byte[orderedIds.size()][];
            for (int i = 0; i < orderedIds.size(); i++) {
                members[i] = orderedIds.get(i).toString().getBytes(StandardCharsets.UTF_8);
            }
            byte[] rawKey = key.getBytes(StandardCharsets.UTF_8);
            List<Double> scores = redisTemplate.execute(connection ->
                    connection.zSetCommands().zMScore(rawKey, members), true);
            if (scores == null || scores.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<Long, Integer> result = new HashMap<>(orderedIds.size());
            for (int i = 0; i < orderedIds.size() && i < scores.size(); i++) {
                Double score = scores.get(i);
                if (score != null) {
                    result.put(orderedIds.get(i), score.intValue());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Redis getScores failed. key={}", key, e);
            return Collections.emptyMap();
        }
    }

    private void clampScoreAtZero(String key, Long boothId) {
        try {
            if (redisTemplate == null) {
                return;
            }
            redisTemplate.execute(CLAMP_ZERO_SCRIPT, List.of(key), boothId.toString());
        } catch (Exception e) {
            log.warn("Redis clampScoreAtZero failed. key={}, boothId={}", key, boothId, e);
        }
    }

    private String likedKey(Long boothId) {
        return BOOTH_LIKED_KEY_PREFIX + boothId;
    }

    public record RedisChangeResult(
            boolean available,
            boolean changed
    ) {
        public static RedisChangeResult changedResult() {
            return new RedisChangeResult(true, true);
        }

        public static RedisChangeResult unchangedResult() {
            return new RedisChangeResult(true, false);
        }

        public static RedisChangeResult unavailableResult() {
            return new RedisChangeResult(false, false);
        }
    }
}
