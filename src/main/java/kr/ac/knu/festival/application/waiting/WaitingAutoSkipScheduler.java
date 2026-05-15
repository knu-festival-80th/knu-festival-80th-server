package kr.ac.knu.festival.application.waiting;

import kr.ac.knu.festival.application.booth.BoothRankingStreamService;
import kr.ac.knu.festival.domain.waiting.entity.Waiting;
import kr.ac.knu.festival.domain.waiting.repository.WaitingRepository;
import kr.ac.knu.festival.infra.redis.BoothRankingRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingAutoSkipScheduler {

    private static final int SKIP_THRESHOLD_MINUTES = 10;

    private final WaitingRepository waitingRepository;
    private final BoothRankingRedisRepository boothRankingRedisRepository;
    private final BoothRankingStreamService boothRankingStreamService;
    private final WaitingCommandService waitingCommandService;

    @Scheduled(fixedDelay = 60_000L)
    @Transactional
    public void autoSkipExpiredCalls() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(SKIP_THRESHOLD_MINUTES);

        List<Waiting> expiredWaitings = waitingRepository.findExpiredCalls(threshold);
        if (expiredWaitings.isEmpty()) {
            return;
        }

        List<Long> expiredIds = expiredWaitings.stream().map(Waiting::getId).toList();
        // UPDATE 가 status='CALLED' 인 행만 갱신하도록 강제해 ENTER 와 race 가 발생해도 잘못 SKIPPED 처리되지 않는다.
        int skipped = waitingRepository.skipExpiredCalls(expiredIds);

        // 실제 SKIPPED 로 전이된 행만 SMS·카운터 감소 대상에 포함시킨다.
        List<Waiting> actuallySkipped = (skipped == expiredWaitings.size())
                ? expiredWaitings
                : waitingRepository.findSkippedByIds(expiredIds);

        Map<Long, Long> decrementByBooth = new HashMap<>();
        for (Waiting waiting : actuallySkipped) {
            Long boothId = waiting.getBooth().getId();
            decrementByBooth.merge(boothId, 1L, Long::sum);
        }

        afterCommit(() -> {
            for (Waiting waiting : actuallySkipped) {
                String message = "[%s] %d번 대기가 시간 초과로 취소되었습니다."
                        .formatted(waiting.getBooth().getName(), waiting.getWaitingNumber());
                // SmsStatusUpdater 가 별도 트랜잭션에서 row 를 조회하려면 본 commit 이 끝난 뒤여야 한다.
                waitingCommandService.sendSmsAsync(waiting.getId(), waiting.getPhoneNumber(), message);
            }
            decrementByBooth.forEach(boothRankingRedisRepository::decrementWaitingCount);
            if (!actuallySkipped.isEmpty()) {
                boothRankingStreamService.markDirty();
            }
        });
        log.info("Auto-skipped {} expired waitings (threshold={}, candidates={})",
                skipped, threshold, expiredWaitings.size());
    }

    private void afterCommit(Runnable runnable) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runnable.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runnable.run();
            }
        });
    }
}
