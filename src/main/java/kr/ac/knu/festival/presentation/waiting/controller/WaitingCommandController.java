package kr.ac.knu.festival.presentation.waiting.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import kr.ac.knu.festival.application.waiting.WaitingCommandService;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.waiting.controller.docs.WaitingCommandControllerDocs;
import kr.ac.knu.festival.presentation.waiting.dto.request.WaitingCreateRequest;
import kr.ac.knu.festival.presentation.waiting.dto.request.WaitingInsertRequest;
import kr.ac.knu.festival.presentation.waiting.dto.request.WaitingReorderRequest;
import kr.ac.knu.festival.presentation.waiting.dto.request.WaitingToggleRequest;
import kr.ac.knu.festival.presentation.waiting.dto.response.WaitingRegisterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping
public class WaitingCommandController implements WaitingCommandControllerDocs {

    private final WaitingCommandService waitingCommandService;

    @Override
    @PostMapping("/api/v1/booths/{booth-id}/waitings")
    public ResponseEntity<ApiResponse<WaitingRegisterResponse>> registerWaiting(
            @PathVariable("booth-id") Long boothId,
            @RequestBody @Valid WaitingCreateRequest request
    ) {
        WaitingRegisterResponse result = waitingCommandService.registerWaiting(boothId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Override
    @DeleteMapping("/api/v1/waitings/{waiting-id}")
    public ResponseEntity<ApiResponse<Void>> cancelWaitingByOwner(
            @PathVariable("waiting-id") Long waitingId,
            @RequestParam("phoneLast4")
            @NotBlank
            @Pattern(regexp = "^\\d{4}$", message = "전화번호 뒤 4자리를 입력해주세요.")
            String phoneLast4
    ) {
        waitingCommandService.cancelWaitingByOwner(waitingId, phoneLast4);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PatchMapping("/admin/v1/waitings/{waiting-id}/call")
    public ResponseEntity<ApiResponse<Void>> callWaiting(
            @PathVariable("waiting-id") Long waitingId
    ) {
        waitingCommandService.callWaiting(waitingId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PatchMapping("/admin/v1/waitings/{waiting-id}/enter")
    public ResponseEntity<ApiResponse<Void>> enterWaiting(
            @PathVariable("waiting-id") Long waitingId
    ) {
        waitingCommandService.enterWaiting(waitingId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PatchMapping("/admin/v1/waitings/{waiting-id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelWaiting(
            @PathVariable("waiting-id") Long waitingId
    ) {
        waitingCommandService.cancelWaiting(waitingId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PatchMapping("/admin/v1/waitings/{waiting-id}/skip")
    public ResponseEntity<ApiResponse<Void>> skipWaiting(
            @PathVariable("waiting-id") Long waitingId
    ) {
        waitingCommandService.skipWaiting(waitingId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PostMapping("/admin/v1/booths/{booth-id}/waitings/insert")
    public ResponseEntity<ApiResponse<WaitingRegisterResponse>> insertWaiting(
            @PathVariable("booth-id") Long boothId,
            @RequestBody @Valid WaitingInsertRequest request
    ) {
        WaitingRegisterResponse result = waitingCommandService.insertWaiting(boothId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Override
    @PatchMapping("/admin/v1/waitings/{waiting-id}/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderWaiting(
            @PathVariable("waiting-id") Long waitingId,
            @RequestBody @Valid WaitingReorderRequest request
    ) {
        waitingCommandService.reorderWaiting(waitingId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PatchMapping("/admin/v1/booths/{booth-id}/waitings/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleBoothWaiting(
            @PathVariable("booth-id") Long boothId,
            @RequestBody @Valid WaitingToggleRequest request
    ) {
        waitingCommandService.toggleBoothWaiting(boothId, request.open());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PostMapping("/admin/v1/waitings/{waiting-id}/resend-sms")
    public ResponseEntity<ApiResponse<Void>> resendSms(
            @PathVariable("waiting-id") Long waitingId
    ) {
        waitingCommandService.resendSms(waitingId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
