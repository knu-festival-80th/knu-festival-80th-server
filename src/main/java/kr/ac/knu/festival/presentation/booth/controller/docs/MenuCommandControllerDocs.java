package kr.ac.knu.festival.presentation.booth.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.booth.dto.request.MenuCreateRequest;
import kr.ac.knu.festival.presentation.booth.dto.request.MenuUpdateRequest;
import kr.ac.knu.festival.presentation.booth.dto.response.MenuResponse;
import org.springframework.http.ResponseEntity;
import java.util.List;

@Tag(name = "메뉴 Command/Query", description = "부스 메뉴 관리 API (관리자)")
public interface MenuCommandControllerDocs {

    @Operation(summary = "메뉴 목록 조회", description = "관리자용 메뉴 목록 조회")
    ResponseEntity<ApiResponse<List<MenuResponse>>> getMenus(Long boothId);

    @Operation(summary = "메뉴 등록")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "부스 없음")
    })
    ResponseEntity<ApiResponse<MenuResponse>> createMenu(Long boothId, MenuCreateRequest request);

    @Operation(summary = "메뉴 수정")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "메뉴 없음")
    })
    ResponseEntity<ApiResponse<MenuResponse>> updateMenu(Long boothId, Long menuId, MenuUpdateRequest request);

    @Operation(summary = "메뉴 품절 토글")
    ResponseEntity<ApiResponse<MenuResponse>> toggleSoldOut(Long boothId, Long menuId);

    @Operation(summary = "메뉴 삭제")
    ResponseEntity<ApiResponse<Void>> deleteMenu(Long boothId, Long menuId);
}
