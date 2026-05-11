package kr.ac.knu.festival.application.canvas;

import kr.ac.knu.festival.domain.canvas.entity.CanvasBoard;
import kr.ac.knu.festival.domain.canvas.repository.CanvasBoardRepository;
import kr.ac.knu.festival.domain.canvas.repository.CanvasPostitRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.presentation.canvas.dto.response.BoardSummaryResponse;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasPostitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CanvasQueryService {

    private final CanvasBoardRepository canvasBoardRepository;
    private final CanvasPostitRepository canvasPostitRepository;

    public List<CanvasPostitResponse> getPostits(Long boardId) {
        CanvasBoard board = boardId != null
                ? canvasBoardRepository.findById(boardId)
                        .orElseThrow(() -> new BusinessException(BusinessErrorCode.CANVAS_BOARD_NOT_FOUND))
                : canvasBoardRepository.findLatestBoard()
                        .orElseThrow(() -> new BusinessException(BusinessErrorCode.CANVAS_BOARD_NOT_FOUND));

        return canvasPostitRepository.findAllByBoardOrderByIdAsc(board)
                .stream()
                .map(CanvasPostitResponse::fromEntity)
                .toList();
    }

    public List<BoardSummaryResponse> getBoardSummaries() {
        return canvasBoardRepository.findBoardSummaries()
                .stream()
                .map(row -> new BoardSummaryResponse(
                        ((CanvasBoard) row[0]).getId(),
                        ((CanvasBoard) row[0]).getBoardVariant(),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }
}
