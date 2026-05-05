package kr.ac.knu.festival.presentation.matching.controller;

import jakarta.validation.Valid;
import kr.ac.knu.festival.application.matching.MatchingCommandService;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.auth.CurrentAdmin;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.matching.controller.docs.MatchingCommandControllerDocs;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingAuthRequest;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingCreateRequest;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingStatusUpdateRequest;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingJobResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingRegisterResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class MatchingCommandController implements MatchingCommandControllerDocs {

    private final MatchingCommandService matchingCommandService;

    @Override
    @PostMapping("/matchings")
    public ResponseEntity<ApiResponse<MatchingRegisterResponse>> register(
            @RequestBody @Valid MatchingCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(matchingCommandService.register(request)));
    }

    @Override
    @DeleteMapping("/matchings")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @RequestBody @Valid MatchingAuthRequest request
    ) {
        matchingCommandService.cancel(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Override
    @PostMapping("/admin/matching-jobs")
    public ResponseEntity<ApiResponse<MatchingJobResponse>> runMatchingJob(
            @CurrentAdmin AdminInfo admin
    ) {
        admin.requireSuperAdmin();
        return ResponseEntity.ok(ApiResponse.success(matchingCommandService.runMatchingJob()));
    }

    @Override
    @PatchMapping("/admin/matchings/status")
    public ResponseEntity<ApiResponse<MatchingStatusResponse>> updateStatus(
            @CurrentAdmin AdminInfo admin,
            @RequestBody @Valid MatchingStatusUpdateRequest request
    ) {
        admin.requireSuperAdmin();
        return ResponseEntity.ok(ApiResponse.success(matchingCommandService.updateStatus(request)));
    }
}
