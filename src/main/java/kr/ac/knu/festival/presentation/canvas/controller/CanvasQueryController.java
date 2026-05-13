package kr.ac.knu.festival.presentation.canvas.controller;

import kr.ac.knu.festival.application.canvas.CanvasQueryService;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.canvas.controller.docs.CanvasQueryControllerDocs;
import kr.ac.knu.festival.presentation.canvas.dto.response.BoardSummaryResponse;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasBoardQuestionResponse;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasPostitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/canvas")
public class CanvasQueryController implements CanvasQueryControllerDocs {

    private final CanvasQueryService canvasQueryService;

    @Override
    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<List<CanvasBoardQuestionResponse>>> getQuestions() {
        return ResponseEntity.ok(ApiResponse.success(canvasQueryService.getQuestions()));
    }

    @Override
    @GetMapping("/boards")
    public ResponseEntity<ApiResponse<List<BoardSummaryResponse>>> getBoardSummaries(
            @RequestParam("questionId") Long questionId
    ) {
        return ResponseEntity.ok(ApiResponse.success(canvasQueryService.getBoardSummaries(questionId)));
    }

    @Override
    @GetMapping("/postits")
    public ResponseEntity<ApiResponse<List<CanvasPostitResponse>>> getPostits(
            @RequestParam("boardId") Long boardId
    ) {
        return ResponseEntity.ok(ApiResponse.success(canvasQueryService.getPostits(boardId)));
    }
}
