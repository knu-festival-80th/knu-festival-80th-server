package kr.ac.knu.festival.application.canvas;

import kr.ac.knu.festival.domain.canvas.entity.CanvasBoard;
import kr.ac.knu.festival.domain.canvas.repository.CanvasBoardQuestionRepository;
import kr.ac.knu.festival.domain.canvas.repository.CanvasBoardRepository;
import kr.ac.knu.festival.domain.canvas.repository.CanvasPostitRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.presentation.canvas.dto.response.BoardSummaryResponse;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasBoardQuestionResponse;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasPostitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CanvasQueryService {

    private final CanvasBoardQuestionRepository canvasBoardQuestionRepository;
    private final CanvasBoardRepository canvasBoardRepository;
    private final CanvasPostitRepository canvasPostitRepository;

    public List<CanvasBoardQuestionResponse> getQuestions() {
        return canvasBoardQuestionRepository.findAllByOrderByOrderIndexAsc()
                .stream()
                .map(CanvasBoardQuestionResponse::fromEntity)
                .toList();
    }

    public List<BoardSummaryResponse> getBoardSummaries(Long questionId) {
        if (!canvasBoardQuestionRepository.existsById(questionId)) {
            throw new BusinessException(BusinessErrorCode.CANVAS_QUESTION_NOT_FOUND);
        }
        return canvasBoardRepository.findBoardSummariesByQuestion(questionId)
                .stream()
                .map(row -> {
                    CanvasBoard board = (CanvasBoard) row[0];
                    return new BoardSummaryResponse(
                            board.getId(),
                            board.getQuestion().getId(),
                            board.getQuestion().getBoardVariant(),
                            ((Number) row[1]).longValue(),
                            board.getMaxNoteCount()
                    );
                })
                .toList();
    }

    public List<CanvasPostitResponse> getPostits(Long boardId) {
        CanvasBoard board = canvasBoardRepository.findById(boardId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CANVAS_BOARD_NOT_FOUND));
        return canvasPostitRepository.findAllByBoardOrderByIdAsc(board)
                .stream()
                .map(CanvasPostitResponse::fromEntity)
                .toList();
    }
}
