package kr.ac.knu.festival.presentation.canvas.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasPostitResponse;
import kr.ac.knu.festival.presentation.canvas.dto.response.ZoneSummaryResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Canvas Query", description = "포스트잇 목록 / 존 요약 조회 (사용자)")
public interface CanvasQueryControllerDocs {

    @Operation(summary = "포스트잇 목록 조회", description = "존 번호로 포스트잇 목록을 조회합니다. zone 파라미터 미입력 시 현재 활성 존을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<CanvasPostitResponse>>> getPostits(
            @Parameter(description = "존 번호 (1부터 시작, 미입력 시 현재 활성 존)") Integer zone
    );

    @Operation(summary = "존 요약 조회", description = "각 존의 포스트잇 수를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<ZoneSummaryResponse>>> getZoneSummaries();
}
