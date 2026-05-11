package kr.ac.knu.festival.presentation.matching.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knu.festival.application.matching.MatchingQueryService;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.matching.controller.docs.MatchingQueryControllerDocs;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingAuthRequest;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingResultResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingStatusResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.UnmatchedParticipantsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @GetMapping("/matchings/unmatched")
    public ResponseEntity<ApiResponse<UnmatchedParticipantsResponse>> getUnmatchedParticipants() {
        return ResponseEntity.ok(ApiResponse.success(matchingQueryService.getUnmatchedParticipants()));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
