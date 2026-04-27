package kr.ac.knu.festival.presentation.waiting.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.waiting.dto.request.WaitingCreateRequest;
import kr.ac.knu.festival.presentation.waiting.dto.request.WaitingInsertRequest;
import kr.ac.knu.festival.presentation.waiting.dto.request.WaitingReorderRequest;
import kr.ac.knu.festival.presentation.waiting.dto.request.WaitingToggleRequest;
import kr.ac.knu.festival.presentation.waiting.dto.response.WaitingRegisterResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "대기열 Command", description = "웹 대기 등록·본인 취소 + 관리자 대기열 운영 API")
public interface WaitingCommandControllerDocs {

    @Operation(summary = "대기 등록 (웹)", description = "손님이 축제 웹앱에서 직접 대기 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "대기 접수 중단"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "동일 부스 동일 전화번호 중복")
    })
    ResponseEntity<ApiResponse<WaitingRegisterResponse>> registerWaiting(Long boothId, WaitingCreateRequest request);

    @Operation(summary = "본인 대기 취소", description = "전화번호 뒤 4자리로 본인 확인 후 취소")
    ResponseEntity<ApiResponse<Void>> cancelWaitingByOwner(Long waitingId, String phoneLast4);

    @Operation(summary = "대기팀 호출", description = "관리자가 대기팀을 호출합니다. SMS 발송 비동기.")
    ResponseEntity<ApiResponse<Void>> callWaiting(@Parameter(hidden = true) AdminInfo admin, Long waitingId);

    @Operation(summary = "입장 완료 처리")
    ResponseEntity<ApiResponse<Void>> enterWaiting(@Parameter(hidden = true) AdminInfo admin, Long waitingId);

    @Operation(summary = "대기 취소 (관리자)")
    ResponseEntity<ApiResponse<Void>> cancelWaiting(@Parameter(hidden = true) AdminInfo admin, Long waitingId);

    @Operation(summary = "미방문 건너뛰기")
    ResponseEntity<ApiResponse<Void>> skipWaiting(@Parameter(hidden = true) AdminInfo admin, Long waitingId);

    @Operation(summary = "대기열 중간 삽입")
    ResponseEntity<ApiResponse<WaitingRegisterResponse>> insertWaiting(
            @Parameter(hidden = true) AdminInfo admin, Long boothId, WaitingInsertRequest request);

    @Operation(summary = "대기 순서 변경")
    ResponseEntity<ApiResponse<Void>> reorderWaiting(
            @Parameter(hidden = true) AdminInfo admin, Long waitingId, WaitingReorderRequest request);

    @Operation(summary = "부스 대기 접수 ON/OFF")
    ResponseEntity<ApiResponse<Void>> toggleBoothWaiting(
            @Parameter(hidden = true) AdminInfo admin, Long boothId, WaitingToggleRequest request);

    @Operation(summary = "SMS 재발송")
    ResponseEntity<ApiResponse<Void>> resendSms(@Parameter(hidden = true) AdminInfo admin, Long waitingId);
}
