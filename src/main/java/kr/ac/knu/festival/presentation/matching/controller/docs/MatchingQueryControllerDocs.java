package kr.ac.knu.festival.presentation.matching.controller.docs;

import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingAuthRequest;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingApplicantsCountResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingParticipantsAdminResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingResultResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingStatusResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.UnmatchedParticipantsResponse;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

@Tag(name = "매칭 Query", description = "두근두근 인스타팅 결과·상태 조회 API")
public interface MatchingQueryControllerDocs {

    @Operation(summary = "매칭 결과 조회")
    ResponseEntity<ApiResponse<MatchingResultResponse>> getResult(
            MatchingAuthRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    );

    @Operation(summary = "매칭 서비스 상태 조회")
    ResponseEntity<ApiResponse<MatchingStatusResponse>> getStatus();

    @Operation(summary = "현재 신청자 수 조회 (성별 분리)")
    ResponseEntity<ApiResponse<MatchingApplicantsCountResponse>> getApplicantsCount();

    @Operation(summary = "미매칭 공개 목록 조회")
    ResponseEntity<ApiResponse<UnmatchedParticipantsResponse>> getUnmatchedParticipants();

    @Operation(summary = "[관리자] 신청자 목록 조회 (필터링)")
    ResponseEntity<ApiResponse<MatchingParticipantsAdminResponse>> listParticipantsForAdmin(
            @Parameter(hidden = true) AdminInfo admin,
            @Parameter(description = "축제 일자 (YYYY-MM-DD). 미지정 시 현재 축제일/최근 일자 기준") LocalDate festivalDay,
            @Parameter(description = "참가자 상태") MatchingParticipantStatus status,
            @Parameter(description = "성별") MatchingGender gender,
            @Parameter(description = "instagramId 부분 검색") String search
    );
}
