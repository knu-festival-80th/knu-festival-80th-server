package kr.ac.knu.festival.application.canvas;

import kr.ac.knu.festival.domain.canvas.event.PostitCreatedEvent;
import kr.ac.knu.festival.infra.canvas.GeminiModerationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CanvasModerationEventListener {

    private final GeminiModerationClient geminiModerationClient;
    private final CanvasCommandService canvasCommandService;

    @Async("geminiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostitCreated(PostitCreatedEvent event) {
        try {
            if (!geminiModerationClient.isAppropriate(event.message())) {
                log.info("부적절한 포스트잇 삭제 (postitId={})", event.postitId());
                canvasCommandService.deletePostit(event.postitId());
            }
        } catch (Exception e) {
            log.warn("포스트잇 검열 처리 중 오류 (postitId={}): {}", event.postitId(), e.getMessage());
        }
    }
}
