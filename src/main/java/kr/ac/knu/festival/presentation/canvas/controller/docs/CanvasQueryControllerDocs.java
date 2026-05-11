package kr.ac.knu.festival.presentation.canvas.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.canvas.dto.response.BoardSummaryResponse;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasBoardQuestionResponse;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasPostitResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Canvas Query", description = "문항 목록 / 보드 목록 / 포스트잇 목록 조회")
public interface CanvasQueryControllerDocs {

    @Operation(summary = "문항 목록 조회", description = "롤링페이퍼 문항 전체를 orderIndex 순으로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<CanvasBoardQuestionResponse>>> getQuestions();

    @Operation(summary = "보드 목록 조회", description = "특정 문항에 속한 보드 목록과 각 보드의 포스트잇 수를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "문항 없음")
    })
    ResponseEntity<ApiResponse<List<BoardSummaryResponse>>> getBoardSummaries(
            @Parameter(description = "문항 ID") Long questionId
    );

    @Operation(summary = "포스트잇 목록 조회", description = "특정 보드의 포스트잇 목록을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "보드 없음")
    })
    ResponseEntity<ApiResponse<List<CanvasPostitResponse>>> getPostits(
            @Parameter(description = "보드 ID") Long boardId
    );
}
