package kr.ac.knu.festival.application.canvas;

import kr.ac.knu.festival.domain.canvas.entity.CanvasBoard;
import kr.ac.knu.festival.domain.canvas.entity.CanvasPostit;
import kr.ac.knu.festival.domain.canvas.entity.StickerMeta;
import kr.ac.knu.festival.domain.canvas.repository.CanvasBoardRepository;
import kr.ac.knu.festival.domain.canvas.repository.CanvasPostitRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.presentation.canvas.dto.request.CanvasPostitCreateRequest;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasPostitCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CanvasCommandService {

    private static final int MAX_NOTES_PER_BOARD = 100;
    private static final double BOARD_SIZE = 852.0;
    private static final double COLLISION_SCALE = 0.4;
    private static final double PAD_LEFT = 14.0;
    private static final double PAD_RIGHT = 14.0;
    private static final double PAD_TOP = 20.0;
    private static final double PAD_BOTTOM = 20.0;
    private static final double FRAME_SIZE = 320.0;
    private static final double FRAME_BLOCKED_PADDING = 26.0;

    private final CanvasBoardRepository canvasBoardRepository;
    private final CanvasPostitRepository canvasPostitRepository;

    public CanvasPostitCreateResponse createPostit(CanvasPostitCreateRequest request) {
        CanvasBoard board = canvasBoardRepository.findById(request.boardId())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CANVAS_BOARD_NOT_FOUND));

        if (canvasPostitRepository.countByBoard(board) >= MAX_NOTES_PER_BOARD) {
            throw new BusinessException(BusinessErrorCode.CANVAS_BOARD_FULL);
        }

        StickerMeta meta = StickerMeta.of(request.colorId());
        double x = request.placement().x();
        double y = request.placement().y();

        validateBoardBoundary(x, y, meta);
        validateFrameArea(x, y, meta);

        List<CanvasPostit> existingPostits = canvasPostitRepository.findAllByBoardOrderByIdAsc(board);
        validateNoCollision(x, y, meta, existingPostits);

        CanvasPostit postit = CanvasPostit.createCanvasPostit(board, request.colorId(), request.message(), x, y);
        canvasPostitRepository.save(postit);
        return CanvasPostitCreateResponse.fromEntity(postit);
    }

    public Long createBoard() {
        CanvasBoard board = canvasBoardRepository.save(CanvasBoard.create());
        return board.getId();
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
