package kr.ac.knu.festival.presentation.matching.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knu.festival.application.matching.MatchingQueryService;
import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.auth.CurrentAdmin;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.matching.controller.docs.MatchingQueryControllerDocs;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingAuthRequest;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingApplicantsCountResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingParticipantsAdminResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingResultResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingStatusResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.UnmatchedParticipantsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class MatchingQueryController implements MatchingQueryControllerDocs {

    private final MatchingQueryService matchingQueryService;

    @Override
    @PostMapping("/matchings/result")
    public ResponseEntity<ApiResponse<MatchingResultResponse>> getResult(
            @RequestBody @Valid MatchingAuthRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(ApiResponse.success(matchingQueryService.getResult(request, clientIp(httpRequest))));
    }

    @Override
    @GetMapping("/matchings/status")
    public ResponseEntity<ApiResponse<MatchingStatusResponse>> getStatus() {
        return ResponseEntity.ok(ApiResponse.success(matchingQueryService.getStatus()));
    }

    @Override
    @GetMapping("/matchings/applicants/count")
    public ResponseEntity<ApiResponse<MatchingApplicantsCountResponse>> getApplicantsCount() {
        return ResponseEntity.ok(ApiResponse.success(matchingQueryService.getApplicantsCount()));
    }

    @Override
    @GetMapping("/matchings/unmatched")
    public ResponseEntity<ApiResponse<UnmatchedParticipantsResponse>> getUnmatchedParticipants() {
        return ResponseEntity.ok(ApiResponse.success(matchingQueryService.getUnmatchedParticipants()));
    }

    @Override
    @GetMapping("/admin/matchings/participants")
    public ResponseEntity<ApiResponse<MatchingParticipantsAdminResponse>> listParticipantsForAdmin(
            @CurrentAdmin AdminInfo admin,
            @RequestParam(value = "festivalDay", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate festivalDay,
            @RequestParam(value = "status", required = false) MatchingParticipantStatus status,
            @RequestParam(value = "gender", required = false) MatchingGender gender,
            @RequestParam(value = "search", required = false) String search
    ) {
        admin.requireSuperAdmin();
        return ResponseEntity.ok(ApiResponse.success(
                matchingQueryService.listParticipantsForAdmin(festivalDay, status, gender, search)
        ));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
