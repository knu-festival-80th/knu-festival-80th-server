package kr.ac.knu.festival.application.canvas;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import kr.ac.knu.festival.domain.canvas.event.PostitCreatedEvent;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.infra.canvas.GeminiModerationClient;
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

    private static final String METRIC_DECISION = "festival.canvas.moderation.decision";

    private final GeminiModerationClient geminiModerationClient;
    private final CanvasCommandService canvasCommandService;
    private final ThreadPoolTaskExecutor geminiExecutor;
    private final MeterRegistry meterRegistry;

    public CanvasModerationEventListener(
            GeminiModerationClient geminiModerationClient,
            CanvasCommandService canvasCommandService,
            @Qualifier("geminiExecutor") ThreadPoolTaskExecutor geminiExecutor,
            MeterRegistry meterRegistry) {
        this.geminiModerationClient = geminiModerationClient;
        this.canvasCommandService = canvasCommandService;
        this.geminiExecutor = geminiExecutor;
        this.meterRegistry = meterRegistry;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostitCreated(PostitCreatedEvent event) {
        try {
            geminiExecutor.execute(() -> processModeration(event));
        } catch (RejectedExecutionException e) {
            log.warn("[gemini-fail-open] postitId={} status=executor-rejected reason={}",
                    event.postitId(), e.getClass().getSimpleName());
            failOpen(event.postitId());
        }
    }

    /**
     * 검열 결과 판정과 DB 갱신을 분리해 실패 원인을 명확히 구분한다.
     * <ul>
     *     <li>Gemini 호출 실패 → fail-open(APPROVE) 결정 후 DB 갱신 단계로 진행</li>
     *     <li>DB 갱신 실패 → 별도 메트릭 로그 + 포스트잇 누락은 debug</li>
     * </ul>
     */
    private void processModeration(PostitCreatedEvent event) {
        boolean shouldApprove;
        try {
            shouldApprove = geminiModerationClient.isAppropriate(event.message());
        } catch (Exception e) {
            log.warn("[gemini-fail-open] postitId={} status=client-error reason={}",
                    event.postitId(), e.getClass().getSimpleName());
            shouldApprove = true;
        }

        applyDecision(event.postitId(), shouldApprove);
    }

    private void applyDecision(Long postitId, boolean approve) {
        Counter.builder(METRIC_DECISION)
                .tag("verdict", approve ? "APPROVE" : "REJECT")
                .register(meterRegistry)
                .increment();
        try {
            if (approve) {
                canvasCommandService.approvePostit(postitId);
            } else {
                log.info("부적절한 포스트잇 거부 (postitId={})", postitId);
                canvasCommandService.rejectPostit(postitId);
            }
        } catch (BusinessException e) {
            if (e.getErrorCode() == BusinessErrorCode.CANVAS_POSTIT_NOT_FOUND) {
                // 관리자가 검열 도중 포스트잇을 삭제한 경우. 정상 스킵.
                log.debug("[moderation-skip] postitId={} reason=postit-deleted", postitId);
                return;
            }
            log.warn("[moderation-failover] postitId={} reason={}", postitId, e.getClass().getSimpleName());
        } catch (Exception e) {
            // 락 타임아웃 / DB 커넥션 오류 등.
            log.warn("[moderation-failover] postitId={} reason={}", postitId, e.getClass().getSimpleName());
        }
    }

    private void failOpen(Long postitId) {
        applyDecision(postitId, true);
    }
}
