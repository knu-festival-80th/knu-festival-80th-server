package kr.ac.knu.festival.application.canvas;

import kr.ac.knu.festival.domain.canvas.event.PostitCreatedEvent;
import kr.ac.knu.festival.infra.canvas.GeminiModerationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Component
public class CanvasModerationEventListener {

    private final GeminiModerationClient geminiModerationClient;
    private final CanvasCommandService canvasCommandService;
    private final ThreadPoolTaskExecutor geminiExecutor;

    public CanvasModerationEventListener(
            GeminiModerationClient geminiModerationClient,
            CanvasCommandService canvasCommandService,
            @Qualifier("geminiExecutor") ThreadPoolTaskExecutor geminiExecutor) {
        this.geminiModerationClient = geminiModerationClient;
        this.canvasCommandService = canvasCommandService;
        this.geminiExecutor = geminiExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostitCreated(PostitCreatedEvent event) {
        try {
            geminiExecutor.execute(() -> processModeration(event));
        } catch (RejectedExecutionException e) {
            log.warn("geminiExecutor 포화 — fail-open으로 APPROVED 처리 (postitId={})", event.postitId());
            failOpen(event.postitId());
        }
    }

    private void processModeration(PostitCreatedEvent event) {
        try {
            if (geminiModerationClient.isAppropriate(event.message())) {
                canvasCommandService.approvePostit(event.postitId());
            } else {
                log.info("부적절한 포스트잇 거부 (postitId={})", event.postitId());
                canvasCommandService.rejectPostit(event.postitId());
            }
        } catch (Exception e) {
            log.warn("포스트잇 검열 처리 중 오류 — fail-open으로 APPROVED 처리 (postitId={}): {}", event.postitId(), e.getMessage());
            failOpen(event.postitId());
        }
    }

    private void failOpen(Long postitId) {
        try {
            canvasCommandService.approvePostit(postitId);
        } catch (Exception e) {
            log.warn("포스트잇 fail-open 승인 실패 (postitId={}): {}", postitId, e.getMessage());
        }
    }
}
