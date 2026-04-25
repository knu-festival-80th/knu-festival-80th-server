package kr.ac.knu.festival.presentation.waiting.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import kr.ac.knu.festival.application.waiting.WaitingQueryService;
import kr.ac.knu.festival.domain.waiting.entity.WaitingStatus;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.waiting.controller.docs.WaitingQueryControllerDocs;
import kr.ac.knu.festival.presentation.waiting.dto.response.MyWaitingResponse;
import kr.ac.knu.festival.presentation.waiting.dto.response.WaitingResponse;
import kr.ac.knu.festival.presentation.waiting.dto.response.WaitingStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping
public class WaitingQueryController implements WaitingQueryControllerDocs {

    private final WaitingQueryService waitingQueryService;

    @Override
    @GetMapping("/api/v1/booths/{booth-id}/waitings/status")
    public ResponseEntity<ApiResponse<WaitingStatusResponse>> getBoothStatus(
            @PathVariable("booth-id") Long boothId
    ) {
        return ResponseEntity.ok(ApiResponse.success(waitingQueryService.getBoothStatus(boothId)));
    }

    @Override
    @GetMapping("/api/v1/waitings/{waiting-id}")
    public ResponseEntity<ApiResponse<MyWaitingResponse>> getMyWaiting(
            @PathVariable("waiting-id") Long waitingId,
            @RequestParam("phoneLast4") @NotBlank @Pattern(regexp = "^\\d{4}$", message = "전화번호 뒤 4자리를 입력해주세요.") String phoneLast4
    ) {
        return ResponseEntity.ok(ApiResponse.success(waitingQueryService.getMyWaiting(waitingId, phoneLast4)));
    }

    @Override
    @GetMapping("/admin/v1/booths/{booth-id}/waitings")
    public ResponseEntity<ApiResponse<List<WaitingResponse>>> getWaitings(
            @PathVariable("booth-id") Long boothId,
            @RequestParam(value = "status", required = false) WaitingStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.success(waitingQueryService.getWaitings(boothId, status)));
    }
}
