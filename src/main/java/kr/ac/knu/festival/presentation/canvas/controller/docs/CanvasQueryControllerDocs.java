package kr.ac.knu.festival.presentation.canvas.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.canvas.dto.response.BoardSummaryResponse;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasPostitResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Canvas Query", description = "포스트잇 목록 / 보드 요약 조회 (사용자)")
public interface CanvasQueryControllerDocs {

    @Operation(summary = "포스트잇 목록 조회", description = "boardId로 해당 보드의 포스트잇 목록을 조회합니다. boardId 미입력 시 가장 최근 보드를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "보드 없음")
    })
    ResponseEntity<ApiResponse<List<CanvasPostitResponse>>> getPostits(
            @Parameter(description = "보드 ID (미입력 시 최신 보드)") Long boardId
    );

    @Operation(summary = "보드 요약 조회", description = "각 보드의 포스트잇 수와 디자인 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<BoardSummaryResponse>>> getBoardSummaries();
}
