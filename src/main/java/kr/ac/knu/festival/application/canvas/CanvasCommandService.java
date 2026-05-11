package kr.ac.knu.festival.application.canvas;

import kr.ac.knu.festival.domain.canvas.entity.CanvasBoard;
import kr.ac.knu.festival.domain.canvas.entity.CanvasPostit;
import kr.ac.knu.festival.domain.canvas.repository.CanvasBoardRepository;
import kr.ac.knu.festival.domain.canvas.repository.CanvasPostitRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.presentation.canvas.dto.request.CanvasPostitCreateRequest;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasPostitCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CanvasCommandService {

    private static final int MAX_NOTES_PER_BOARD = 100;

    private final CanvasBoardRepository canvasBoardRepository;
    private final CanvasPostitRepository canvasPostitRepository;

    public CanvasPostitCreateResponse createPostit(CanvasPostitCreateRequest request) {
        CanvasBoard board = canvasBoardRepository.findById(request.boardId())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CANVAS_BOARD_NOT_FOUND));

        if (canvasPostitRepository.countByBoard(board) >= MAX_NOTES_PER_BOARD) {
            throw new BusinessException(BusinessErrorCode.CANVAS_BOARD_FULL);
        }

        CanvasPostit postit = CanvasPostit.createCanvasPostit(
                board,
                request.colorId(),
                request.message(),
                request.placement().x(),
                request.placement().y()
        );

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
}
