package kr.ac.knu.festival.presentation.booth.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.booth.dto.response.BoothDetailResponse;
import kr.ac.knu.festival.presentation.booth.dto.response.BoothListResponse;
import kr.ac.knu.festival.presentation.booth.dto.response.BoothMapResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "부스 Query", description = "부스 조회 API (사용자)")
public interface BoothQueryControllerDocs {

    @Operation(summary = "부스 목록 조회", description = "좋아요/대기 적은 순으로 정렬된 부스 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<BoothListResponse>>> getBooths(
            @Parameter(description = "정렬 기준: likes(기본) / waiting-asc") String sort
    );

    @Operation(summary = "부스 상세 조회", description = "메뉴 목록과 현재 대기팀 수를 포함한 부스 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "부스 없음")
    })
    ResponseEntity<ApiResponse<BoothDetailResponse>> getBooth(Long boothId);

    @Operation(summary = "지도용 부스 목록 조회", description = "좌표와 이름만 포함된 경량 응답을 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<ApiResponse<List<BoothMapResponse>>> getBoothsForMap();
}
