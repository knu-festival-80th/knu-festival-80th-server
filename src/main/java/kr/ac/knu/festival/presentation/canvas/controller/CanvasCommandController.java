package kr.ac.knu.festival.presentation.canvas.controller;

import jakarta.validation.Valid;
import kr.ac.knu.festival.application.canvas.CanvasCommandService;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.auth.CurrentAdmin;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.canvas.controller.docs.CanvasCommandControllerDocs;
import kr.ac.knu.festival.presentation.canvas.dto.request.CanvasPostitCreateRequest;
import kr.ac.knu.festival.presentation.canvas.dto.response.CanvasPostitCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class CanvasCommandController implements CanvasCommandControllerDocs {

    private final CanvasCommandService canvasCommandService;

    @Override
    @PostMapping("/api/v1/canvas/postits")
    public ResponseEntity<ApiResponse<CanvasPostitCreateResponse>> createPostit(
            @RequestBody @Valid CanvasPostitCreateRequest request
    ) {
        CanvasPostitCreateResponse result = canvasCommandService.createPostit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Override
    @DeleteMapping("/admin/v1/canvas/postits/{postit-id}")
    public ResponseEntity<ApiResponse<Void>> deletePostit(
            @CurrentAdmin AdminInfo admin,
            @PathVariable("postit-id") Long postitId
    ) {
        admin.requireSuperAdmin();
        canvasCommandService.deletePostit(postitId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}