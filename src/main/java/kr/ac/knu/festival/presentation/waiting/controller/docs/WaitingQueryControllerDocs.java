package kr.ac.knu.festival.presentation.waiting.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.knu.festival.domain.waiting.entity.WaitingStatus;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.waiting.dto.response.MyWaitingResponse;
import kr.ac.knu.festival.presentation.waiting.dto.response.WaitingResponse;
import kr.ac.knu.festival.presentation.waiting.dto.response.WaitingStatusResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "대기열 Query", description = "대기열 조회 API")
public interface WaitingQueryControllerDocs {

    @Operation(summary = "현재 대기 현황 조회")
    ResponseEntity<ApiResponse<WaitingStatusResponse>> getBoothStatus(Long boothId);

    @Operation(summary = "내 대기 상태 조회", description = "전화번호 뒤 4자리로 본인 확인")
    ResponseEntity<ApiResponse<MyWaitingResponse>> getMyWaiting(Long waitingId, String phoneLast4);

    @Operation(summary = "대기팀 목록 조회 (관리자)")
    ResponseEntity<ApiResponse<List<WaitingResponse>>> getWaitings(
            @Parameter(hidden = true) AdminInfo admin, Long boothId, WaitingStatus status);
}
