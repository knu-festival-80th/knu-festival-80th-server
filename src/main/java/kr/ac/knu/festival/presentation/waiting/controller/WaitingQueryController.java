package kr.ac.knu.festival.presentation.waiting.controller;

import jakarta.validation.Valid;
import kr.ac.knu.festival.application.waiting.WaitingQueryService;
import kr.ac.knu.festival.domain.waiting.entity.WaitingStatus;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.auth.CurrentAdmin;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.waiting.controller.docs.WaitingQueryControllerDocs;
import kr.ac.knu.festival.presentation.waiting.dto.request.MyWaitingLookupRequest;
import kr.ac.knu.festival.presentation.waiting.dto.response.MyWaitingResponse;
import kr.ac.knu.festival.presentation.waiting.dto.response.WaitingResponse;
import kr.ac.knu.festival.presentation.waiting.dto.response.WaitingStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class WaitingQueryController implements WaitingQueryControllerDocs {

    private final WaitingQueryService waitingQueryService;

    @Override
    @GetMapping("/booths/{booth-id}/waitings/status")
    public ResponseEntity<ApiResponse<WaitingStatusResponse>> getBoothStatus(
            @PathVariable("booth-id") Long boothId
    ) {
        return ResponseEntity.ok(ApiResponse.success(waitingQueryService.getBoothStatus(boothId)));
    }

    @Override
    @GetMapping("/waitings/{waiting-id}")
    public ResponseEntity<ApiResponse<MyWaitingResponse>> getMyWaiting(
            @PathVariable("waiting-id") Long waitingId,
            @RequestParam("phoneLast4") String phoneLast4
    ) {
        return ResponseEntity.ok(ApiResponse.success(waitingQueryService.getMyWaiting(waitingId, phoneLast4)));
    }

    @Override
    @PostMapping("/waitings/my")
    public ResponseEntity<ApiResponse<List<MyWaitingResponse>>> getMyWaitings(
            @RequestBody @Valid MyWaitingLookupRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(waitingQueryService.getMyWaitings(request.name(), request.phoneNumber())));
    }

    @Override
    @GetMapping("/admin/booths/{booth-id}/waitings")
    public ResponseEntity<ApiResponse<List<WaitingResponse>>> getWaitings(
            @CurrentAdmin AdminInfo admin,
            @PathVariable("booth-id") Long boothId,
            @RequestParam(value = "status", required = false) WaitingStatus status
    ) {
        admin.validateBoothAccess(boothId);
        return ResponseEntity.ok(ApiResponse.success(waitingQueryService.getWaitings(boothId, status)));
    }
}
