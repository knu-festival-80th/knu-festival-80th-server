package kr.ac.knu.festival.application.waiting;

import kr.ac.knu.festival.domain.waiting.repository.WaitingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingAutoSkipScheduler {

    private static final int SKIP_THRESHOLD_MINUTES = 5;

    private final WaitingRepository waitingRepository;

    @Scheduled(fixedDelay = 60_000L)
    @Transactional
    public void autoSkipExpiredCalls() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(SKIP_THRESHOLD_MINUTES);
        int skipped = waitingRepository.skipExpiredCalls(threshold);
        if (skipped > 0) {
            log.info("Auto-skipped {} expired waitings (threshold={})", skipped, threshold);
        }
    }
}
