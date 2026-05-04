package kr.ac.knu.festival.presentation.waiting.controller;

import jakarta.validation.Valid;
import kr.ac.knu.festival.application.waiting.WaitingCommandService;
import kr.ac.knu.festival.domain.waiting.entity.Waiting;
import kr.ac.knu.festival.domain.waiting.repository.WaitingRepository;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.auth.CurrentAdmin;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
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
@RequestMapping
public class WaitingCommandController implements WaitingCommandControllerDocs {

    private final WaitingCommandService waitingCommandService;
    private final WaitingRepository waitingRepository;

    @Override
    @PostMapping("/booths/{booth-id}/waitings")
    public ResponseEntity<ApiResponse<WaitingRegisterResponse>> registerWaiting(
            @PathVariable("booth-id") Long boothId,
            @RequestBody @Valid WaitingCreateRequest request
    ) {
        WaitingRegisterResponse result = waitingCommandService.registerWaiting(boothId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Override
    @DeleteMapping("/waitings/{waiting-id}")
    public ResponseEntity<ApiResponse<Void>> cancelWaitingByOwner(
            @PathVariable("waiting-id") Long waitingId,
            @RequestParam("phoneLast4") String phoneLast4
    ) {
        waitingCommandService.cancelWaitingByOwner(waitingId, phoneLast4);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ---- 관리자 API ----

    @Override
    @PatchMapping("/admin/waitings/{waiting-id}/call")
    public ResponseEntity<ApiResponse<Void>> callWaiting(
            @CurrentAdmin AdminInfo admin,
            @PathVariable("waiting-id") Long waitingId
    ) {
        validateWaitingBoothAccess(admin, waitingId);
        waitingCommandService.callWaiting(waitingId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PatchMapping("/admin/waitings/{waiting-id}/enter")
    public ResponseEntity<ApiResponse<Void>> enterWaiting(
            @CurrentAdmin AdminInfo admin,
            @PathVariable("waiting-id") Long waitingId
    ) {
        validateWaitingBoothAccess(admin, waitingId);
        waitingCommandService.enterWaiting(waitingId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PatchMapping("/admin/waitings/{waiting-id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelWaiting(
            @CurrentAdmin AdminInfo admin,
            @PathVariable("waiting-id") Long waitingId
    ) {
        validateWaitingBoothAccess(admin, waitingId);
        waitingCommandService.cancelWaiting(waitingId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PatchMapping("/admin/waitings/{waiting-id}/skip")
    public ResponseEntity<ApiResponse<Void>> skipWaiting(
            @CurrentAdmin AdminInfo admin,
            @PathVariable("waiting-id") Long waitingId
    ) {
        validateWaitingBoothAccess(admin, waitingId);
        waitingCommandService.skipWaiting(waitingId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PostMapping("/admin/booths/{booth-id}/waitings/insert")
    public ResponseEntity<ApiResponse<WaitingRegisterResponse>> insertWaiting(
            @CurrentAdmin AdminInfo admin,
            @PathVariable("booth-id") Long boothId,
            @RequestBody @Valid WaitingInsertRequest request
    ) {
        admin.validateBoothAccess(boothId);
        WaitingRegisterResponse result = waitingCommandService.insertWaiting(boothId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @Override
    @PatchMapping("/admin/waitings/{waiting-id}/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderWaiting(
            @CurrentAdmin AdminInfo admin,
            @PathVariable("waiting-id") Long waitingId,
            @RequestBody @Valid WaitingReorderRequest request
    ) {
        validateWaitingBoothAccess(admin, waitingId);
        waitingCommandService.reorderWaiting(waitingId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PatchMapping("/admin/booths/{booth-id}/waitings/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleBoothWaiting(
            @CurrentAdmin AdminInfo admin,
            @PathVariable("booth-id") Long boothId,
            @RequestBody @Valid WaitingToggleRequest request
    ) {
        admin.validateBoothAccess(boothId);
        waitingCommandService.toggleBoothWaiting(boothId, request.open());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PostMapping("/admin/waitings/{waiting-id}/resend-sms")
    public ResponseEntity<ApiResponse<Void>> resendSms(
            @CurrentAdmin AdminInfo admin,
            @PathVariable("waiting-id") Long waitingId
    ) {
        validateWaitingBoothAccess(admin, waitingId);
        waitingCommandService.resendSms(waitingId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    private void validateWaitingBoothAccess(AdminInfo admin, Long waitingId) {
        Waiting waiting = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.WAITING_NOT_FOUND));
        admin.validateBoothAccess(waiting.getBooth().getId());
    }
}
