package kr.ac.knu.festival.presentation.booth.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.booth.dto.request.BoothCreateRequest;
import kr.ac.knu.festival.presentation.booth.dto.request.BoothUpdateRequest;
import kr.ac.knu.festival.presentation.booth.dto.response.BoothListResponse;
import kr.ac.knu.festival.presentation.booth.dto.response.BoothResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "부스 Command", description = "부스 등록/수정/삭제 API (관리자) + 좋아요 (사용자)")
public interface BoothCommandControllerDocs {

    @Operation(summary = "부스 등록", description = "신규 부스를 등록합니다. 슈퍼 관리자 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    ResponseEntity<ApiResponse<BoothResponse>> createBooth(BoothCreateRequest request);

    @Operation(summary = "부스 수정", description = "부스 정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "부스 없음")
    })
    ResponseEntity<ApiResponse<BoothResponse>> updateBooth(Long boothId, BoothUpdateRequest request);

    @Operation(summary = "부스 삭제", description = "대기 중인 팀이 없을 때만 삭제할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "대기 중인 팀이 있어 삭제 불가"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "부스 없음")
    })
    ResponseEntity<ApiResponse<Void>> deleteBooth(Long boothId);

    @Operation(summary = "관리자용 부스 목록 조회", description = "관리자 전용 부스 목록 조회")
    ResponseEntity<ApiResponse<List<BoothListResponse>>> getBoothsForAdmin(String sort);

    @Operation(summary = "부스 좋아요", description = "부스 좋아요 수를 증가시킵니다.")
    ResponseEntity<ApiResponse<BoothResponse>> likeBooth(Long boothId);

    @Operation(summary = "부스 좋아요 취소", description = "부스 좋아요 수를 감소시킵니다.")
    ResponseEntity<ApiResponse<BoothResponse>> unlikeBooth(Long boothId);
}
