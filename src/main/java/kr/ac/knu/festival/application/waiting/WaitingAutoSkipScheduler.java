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
import java.util.List;

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

        var expiredCountByBooth = waitingRepository.countExpiredCallsByBooth(threshold);
        int skipped = waitingRepository.skipExpiredCalls(threshold);

        afterCommit(() -> {
            for (Waiting waiting : expiredWaitings) {
                String message = "[%s] %d번 대기가 시간 초과로 취소되었습니다."
                        .formatted(waiting.getBooth().getName(), waiting.getWaitingNumber());
                waitingCommandService.sendSmsAsync(waiting.getId(), waiting.getPhoneNumber(), message);
            }
            for (Object[] row : expiredCountByBooth) {
                boothRankingRedisRepository.decrementWaitingCount((Long) row[0], (Long) row[1]);
            }
            boothRankingStreamService.markDirty();
        });
        log.info("Auto-skipped {} expired waitings (threshold={})", skipped, threshold);
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
