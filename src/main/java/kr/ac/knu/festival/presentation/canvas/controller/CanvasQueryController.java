package kr.ac.knu.festival.presentation.canvas.controller;

import kr.ac.knu.festival.application.canvas.CanvasQueryService;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.canvas.controller.docs.CanvasQueryControllerDocs;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasPostitResponse;
import kr.ac.knu.festival.presentation.canvas.dto.response.ZoneSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/canvas")
public class CanvasQueryController implements CanvasQueryControllerDocs {

    private final CanvasQueryService canvasQueryService;

    @Override
    @GetMapping("/postits")
    public ResponseEntity<ApiResponse<List<CanvasPostitResponse>>> getPostits(
            @RequestParam(value = "zone", required = false) Integer zone
    ) {
        return ResponseEntity.ok(ApiResponse.success(canvasQueryService.getPostits(zone)));
    }

    @Override
    @GetMapping("/zones")
    public ResponseEntity<ApiResponse<List<ZoneSummaryResponse>>> getZoneSummaries() {
        return ResponseEntity.ok(ApiResponse.success(canvasQueryService.getZoneSummaries()));
    }
}