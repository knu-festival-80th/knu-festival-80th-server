package kr.ac.knu.festival.presentation.booth.controller;

import kr.ac.knu.festival.application.booth.BoothQueryService;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.booth.controller.docs.BoothQueryControllerDocs;
import kr.ac.knu.festival.presentation.booth.dto.response.BoothDetailResponse;
import kr.ac.knu.festival.presentation.booth.dto.response.BoothListResponse;
import kr.ac.knu.festival.presentation.booth.dto.response.BoothMapResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/booths")
@RequiredArgsConstructor
public class BoothQueryController implements BoothQueryControllerDocs {

    private final BoothQueryService boothQueryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<BoothListResponse>>> getBooths(
            @RequestParam(value = "sort", required = false, defaultValue = "likes") String sort
    ) {
        return ResponseEntity.ok(ApiResponse.success(boothQueryService.getBooths(sort)));
    }

    @Override
    @GetMapping("/{booth-id}")
    public ResponseEntity<ApiResponse<BoothDetailResponse>> getBooth(
            @PathVariable("booth-id") Long boothId
    ) {
        return ResponseEntity.ok(ApiResponse.success(boothQueryService.getBooth(boothId)));
    }

    @Override
    @GetMapping("/map")
    public ResponseEntity<ApiResponse<List<BoothMapResponse>>> getBoothsForMap() {
        return ResponseEntity.ok(ApiResponse.success(boothQueryService.getBoothsForMap()));
    }
}
