package kr.ac.knu.festival.presentation.matching.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingAuthRequest;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingCreateRequest;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingStatusUpdateRequest;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingJobResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingRegisterResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingStatusResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "매칭 Command", description = "두근두근 인스타팅 신청·취소 + 관리자 매칭 실행 API")
public interface MatchingCommandControllerDocs {

    @Operation(summary = "매칭 신청")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "신청 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "매칭 신청 중단"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복 신청")
    })
    ResponseEntity<ApiResponse<MatchingRegisterResponse>> register(MatchingCreateRequest request);

    @Operation(summary = "매칭 신청 취소")
    ResponseEntity<ApiResponse<Void>> cancel(MatchingAuthRequest request);

    @Operation(summary = "일괄 매칭 실행")
    ResponseEntity<ApiResponse<MatchingJobResponse>> runMatchingJob(@Parameter(hidden = true) AdminInfo admin);

    @Operation(summary = "매칭 서비스 상태 변경")
    ResponseEntity<ApiResponse<MatchingStatusResponse>> updateStatus(
            @Parameter(hidden = true) AdminInfo admin,
            MatchingStatusUpdateRequest request
    );
}
