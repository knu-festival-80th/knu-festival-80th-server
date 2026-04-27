package kr.ac.knu.festival.presentation.booth.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.booth.dto.request.MenuCreateRequest;
import kr.ac.knu.festival.presentation.booth.dto.request.MenuUpdateRequest;
import kr.ac.knu.festival.presentation.booth.dto.response.MenuResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "메뉴 Command/Query", description = "부스 메뉴 관리 API (관리자)")
public interface MenuCommandControllerDocs {

    @Operation(summary = "메뉴 목록 조회")
    ResponseEntity<ApiResponse<List<MenuResponse>>> getMenus(
            @Parameter(hidden = true) AdminInfo admin, Long boothId);

    @Operation(summary = "메뉴 등록")
    ResponseEntity<ApiResponse<MenuResponse>> createMenu(
            @Parameter(hidden = true) AdminInfo admin, Long boothId, MenuCreateRequest request);

    @Operation(summary = "메뉴 수정")
    ResponseEntity<ApiResponse<MenuResponse>> updateMenu(
            @Parameter(hidden = true) AdminInfo admin, Long boothId, Long menuId, MenuUpdateRequest request);

    @Operation(summary = "메뉴 품절 토글")
    ResponseEntity<ApiResponse<MenuResponse>> toggleSoldOut(
            @Parameter(hidden = true) AdminInfo admin, Long boothId, Long menuId);

    @Operation(summary = "메뉴 삭제")
    ResponseEntity<ApiResponse<Void>> deleteMenu(
            @Parameter(hidden = true) AdminInfo admin, Long boothId, Long menuId);
}
