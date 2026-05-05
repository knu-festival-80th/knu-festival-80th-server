package kr.ac.knu.festival.presentation.booth.controller;

import jakarta.validation.Valid;
import kr.ac.knu.festival.application.booth.MenuCommandService;
import kr.ac.knu.festival.application.booth.MenuQueryService;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.auth.CurrentAdmin;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.booth.controller.docs.MenuCommandControllerDocs;
import kr.ac.knu.festival.presentation.booth.dto.request.MenuCreateRequest;
import kr.ac.knu.festival.presentation.booth.dto.request.MenuUpdateRequest;
import kr.ac.knu.festival.presentation.booth.dto.response.MenuResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/booths/{booth-id}/menus")
public class MenuCommandController implements MenuCommandControllerDocs {

    private final MenuCommandService menuCommandService;
    private final MenuQueryService menuQueryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getMenus(
            @CurrentAdmin AdminInfo admin,
            @PathVariable("booth-id") Long boothId
    ) {
        admin.validateBoothAccess(boothId);
        return ResponseEntity.ok(ApiResponse.success(menuQueryService.getMenus(boothId)));
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<MenuResponse>> createMenu(
            @CurrentAdmin AdminInfo admin,
            @PathVariable("booth-id") Long boothId,
            @RequestBody @Valid MenuCreateRequest request
    ) {
        admin.validateBoothAccess(boothId);
        MenuResponse result = menuCommandService.createMenu(boothId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Override
    @PutMapping("/{menu-id}")
    public ResponseEntity<ApiResponse<MenuResponse>> updateMenu(
            @CurrentAdmin AdminInfo admin,
            @PathVariable("booth-id") Long boothId,
            @PathVariable("menu-id") Long menuId,
            @RequestBody @Valid MenuUpdateRequest request
    ) {
        admin.validateBoothAccess(boothId);
        return ResponseEntity.ok(ApiResponse.success(menuCommandService.updateMenu(boothId, menuId, request)));
    }

    @Override
    @PatchMapping("/{menu-id}/sold-out")
    public ResponseEntity<ApiResponse<MenuResponse>> toggleSoldOut(
            @CurrentAdmin AdminInfo admin,
            @PathVariable("booth-id") Long boothId,
            @PathVariable("menu-id") Long menuId
    ) {
        admin.validateBoothAccess(boothId);
        return ResponseEntity.ok(ApiResponse.success(menuCommandService.toggleSoldOut(boothId, menuId)));
    }

    @Override
    @DeleteMapping("/{menu-id}")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(
            @CurrentAdmin AdminInfo admin,
            @PathVariable("booth-id") Long boothId,
            @PathVariable("menu-id") Long menuId
    ) {
        admin.validateBoothAccess(boothId);
        menuCommandService.deleteMenu(boothId, menuId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
