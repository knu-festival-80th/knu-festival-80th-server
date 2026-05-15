package kr.ac.knu.festival.application.canvas;

import kr.ac.knu.festival.domain.canvas.entity.CanvasBoard;
import kr.ac.knu.festival.domain.canvas.entity.CanvasBoardQuestion;
import kr.ac.knu.festival.domain.canvas.entity.CanvasPostit;
import kr.ac.knu.festival.domain.canvas.entity.ModerationStatus;
import kr.ac.knu.festival.domain.canvas.entity.StickerMeta;
import kr.ac.knu.festival.domain.canvas.event.PostitCreatedEvent;
import kr.ac.knu.festival.domain.canvas.repository.CanvasBoardQuestionRepository;
import kr.ac.knu.festival.domain.canvas.repository.CanvasBoardRepository;
import kr.ac.knu.festival.domain.canvas.repository.CanvasPostitRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.presentation.canvas.dto.request.CanvasPostitCreateRequest;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasPostitCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CanvasCommandService {

    private static final double BOARD_SIZE = 852.0;
    private static final double COLLISION_SCALE = 0.4;
    private static final double PAD_LEFT = 14.0;
    private static final double PAD_RIGHT = 14.0;
    private static final double PAD_TOP = 20.0;
    private static final double PAD_BOTTOM = 20.0;
    private static final double FRAME_SIZE = 320.0;
    private static final double FRAME_BLOCKED_PADDING = 26.0;

    private final CanvasBoardQuestionRepository canvasBoardQuestionRepository;
    private final CanvasBoardRepository canvasBoardRepository;
    private final CanvasPostitRepository canvasPostitRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CanvasPostitCreateResponse createPostit(CanvasPostitCreateRequest request) {
        String sanitized = sanitizeMessage(request.message());

        CanvasBoard board = canvasBoardRepository.findByIdForUpdate(request.boardId())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CANVAS_BOARD_NOT_FOUND));

        if (canvasPostitRepository.countByBoardAndModerationStatusNot(board, ModerationStatus.REJECTED) >= board.getMaxNoteCount()) {
            throw new BusinessException(BusinessErrorCode.CANVAS_BOARD_FULL);
        }

        StickerMeta meta = StickerMeta.of(request.colorId());
        double x = request.placement().x();
        double y = request.placement().y();

        validateBoardBoundary(x, y, meta);
        validateFrameArea(x, y, meta);

        List<CanvasPostit> existingPostits = canvasPostitRepository.findAllByBoardAndModerationStatusNotOrderByIdAsc(board, ModerationStatus.REJECTED);
        validateNoCollision(x, y, meta, existingPostits);

        CanvasPostit postit = CanvasPostit.createCanvasPostit(board, request.colorId(), sanitized, x, y);
        canvasPostitRepository.save(postit);
        eventPublisher.publishEvent(new PostitCreatedEvent(postit.getId(), postit.getMessage()));
        return CanvasPostitCreateResponse.fromEntity(postit);
    }

    /**
     * 메시지 sanitize: null 가드 → trim → HTML 특수문자 escape.
     * trim 후 빈 문자열이면 INVALID_INPUT_VALUE 예외.
     */
    private String sanitizeMessage(String message) {
        if (message == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_INPUT_VALUE);
        }
        String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.INVALID_INPUT_VALUE);
        }
        return trimmed
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public Long createBoard(Long questionId, int maxNoteCount) {
        CanvasBoardQuestion question = canvasBoardQuestionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CANVAS_QUESTION_NOT_FOUND));
        return canvasBoardRepository.save(CanvasBoard.create(question, maxNoteCount)).getId();
    }

    public void approvePostit(Long postitId) {
        CanvasPostit postit = canvasPostitRepository.findById(postitId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CANVAS_POSTIT_NOT_FOUND));
        postit.approve();
    }

    public void rejectPostit(Long postitId) {
        CanvasPostit postit = canvasPostitRepository.findById(postitId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CANVAS_POSTIT_NOT_FOUND));
        postit.reject();
    }

    public void deletePostit(Long postitId) {
        CanvasPostit postit = canvasPostitRepository.findById(postitId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CANVAS_POSTIT_NOT_FOUND));
        canvasPostitRepository.delete(postit);
    }

    private void validateBoardBoundary(double x, double y, StickerMeta meta) {
        double cx = toPixel(x);
        double cy = toPixel(y);
        double halfW = meta.getWidth() / 2;
        double halfH = meta.getHeight() / 2;

        if (cx - halfW < PAD_LEFT || cx + halfW > BOARD_SIZE - PAD_RIGHT
                || cy - halfH < PAD_TOP || cy + halfH > BOARD_SIZE - PAD_BOTTOM) {
            throw new BusinessException(BusinessErrorCode.POSTIT_OUT_OF_BOARD);
        }
    }

    private void validateFrameArea(double x, double y, StickerMeta meta) {
        double frameLeft   = (BOARD_SIZE - FRAME_SIZE) / 2 - FRAME_BLOCKED_PADDING;
        double frameTop    = (BOARD_SIZE - FRAME_SIZE) / 2 - FRAME_BLOCKED_PADDING;
        double frameRight  = (BOARD_SIZE + FRAME_SIZE) / 2 + FRAME_BLOCKED_PADDING;
        double frameBottom = (BOARD_SIZE + FRAME_SIZE) / 2 + FRAME_BLOCKED_PADDING;

        if (overlaps(collisionRect(x, y, meta), new double[]{frameLeft, frameTop, frameRight, frameBottom})) {
            throw new BusinessException(BusinessErrorCode.POSTIT_IN_BLOCKED_AREA);
        }
    }

    private void validateNoCollision(double x, double y, StickerMeta meta, List<CanvasPostit> existingPostits) {
        double[] rect = collisionRect(x, y, meta);
        for (CanvasPostit postit : existingPostits) {
            double[] existing = collisionRect(postit.getPositionX(), postit.getPositionY(), StickerMeta.of(postit.getColorId()));
            if (overlaps(rect, existing)) {
                throw new BusinessException(BusinessErrorCode.POSTIT_POSITION_CONFLICT);
            }
        }
    }

    /** 0~100 상대좌표 → 논리 px */
    private double toPixel(double rel) {
        return (rel / 100.0) * BOARD_SIZE;
    }

    /** collisionScale 적용 충돌 박스: [left, top, right, bottom] */
    private double[] collisionRect(double x, double y, StickerMeta meta) {
        double cx = toPixel(x);
        double cy = toPixel(y);
        double halfW = meta.getWidth() * COLLISION_SCALE / 2;
        double halfH = meta.getHeight() * COLLISION_SCALE / 2;
        return new double[]{cx - halfW, cy - halfH, cx + halfW, cy + halfH};
    }

    /** AABB overlap */
    private boolean overlaps(double[] a, double[] b) {
        return a[0] < b[2] && a[2] > b[0] && a[1] < b[3] && a[3] > b[1];
    }
}
