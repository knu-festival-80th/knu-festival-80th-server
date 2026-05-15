package kr.ac.knu.festival.application.booth;

import kr.ac.knu.festival.domain.booth.repository.BoothRepository;
import kr.ac.knu.festival.infra.redis.BoothRankingRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoothLikeSyncScheduler {

    private final BoothRepository boothRepository;
    private final BoothRankingRedisRepository boothRankingRedisRepository;

    @Scheduled(fixedDelay = 10_000L)
    @Transactional
    public void syncLikeCounts() {
        Map<Long, Integer> dirty = boothRankingRedisRepository.drainDirtyLikeCounts();
        if (!dirty.isEmpty()) {
            dirty.forEach(boothRepository::updateLikeCount);
            log.debug("Synced booth like counts from Redis dirty set. boothCount={}", dirty.size());
        }

        Map<Long, Integer> totalWaitingDirty = boothRankingRedisRepository.drainDirtyTotalWaitingCounts();
        if (!totalWaitingDirty.isEmpty()) {
            totalWaitingDirty.forEach(boothRepository::updateTotalWaitingCount);
            log.debug("Synced booth total waiting counts from Redis dirty set. boothCount={}",
                    totalWaitingDirty.size());
        }
    }
}
