package kr.ac.knu.festival.presentation.booth.controller;

import jakarta.validation.Valid;
import kr.ac.knu.festival.application.booth.BoothCommandService;
import kr.ac.knu.festival.application.booth.BoothQueryService;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.booth.controller.docs.BoothCommandControllerDocs;
import kr.ac.knu.festival.presentation.booth.dto.request.BoothCreateRequest;
import kr.ac.knu.festival.presentation.booth.dto.request.BoothUpdateRequest;
import kr.ac.knu.festival.presentation.booth.dto.response.BoothListResponse;
import kr.ac.knu.festival.presentation.booth.dto.response.BoothResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class BoothCommandController implements BoothCommandControllerDocs {

    private final BoothCommandService boothCommandService;
    private final BoothQueryService boothQueryService;

    @Override
    @PostMapping("/admin/v1/booths")
    public ResponseEntity<ApiResponse<BoothResponse>> createBooth(
            @RequestBody @Valid BoothCreateRequest request
    ) {
        BoothResponse result = boothCommandService.createBooth(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Override
    @PutMapping("/admin/v1/booths/{booth-id}")
    public ResponseEntity<ApiResponse<BoothResponse>> updateBooth(
            @PathVariable("booth-id") Long boothId,
            @RequestBody @Valid BoothUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(boothCommandService.updateBooth(boothId, request)));
    }

    @Override
    @DeleteMapping("/admin/v1/booths/{booth-id}")
    public ResponseEntity<ApiResponse<Void>> deleteBooth(
            @PathVariable("booth-id") Long boothId
    ) {
        boothCommandService.deleteBooth(boothId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @GetMapping("/admin/v1/booths")
    public ResponseEntity<ApiResponse<List<BoothListResponse>>> getBoothsForAdmin(
            @RequestParam(value = "sort", required = false, defaultValue = "likes") String sort
    ) {
        return ResponseEntity.ok(ApiResponse.success(boothQueryService.getBooths(sort)));
    }

    @Override
    @PostMapping("/api/v1/booths/{booth-id}/likes")
    public ResponseEntity<ApiResponse<BoothResponse>> likeBooth(
            @PathVariable("booth-id") Long boothId
    ) {
        return ResponseEntity.ok(ApiResponse.success(boothCommandService.likeBooth(boothId)));
    }

    @Override
    @DeleteMapping("/api/v1/booths/{booth-id}/likes")
    public ResponseEntity<ApiResponse<BoothResponse>> unlikeBooth(
            @PathVariable("booth-id") Long boothId
    ) {
        return ResponseEntity.ok(ApiResponse.success(boothCommandService.unlikeBooth(boothId)));
    }
}
