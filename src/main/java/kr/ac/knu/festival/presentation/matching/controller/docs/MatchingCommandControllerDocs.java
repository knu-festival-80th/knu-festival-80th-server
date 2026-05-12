package kr.ac.knu.festival.presentation.matching.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.ac.knu.festival.global.auth.AdminInfo;
import kr.ac.knu.festival.global.response.ApiResponse;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingCreateRequest;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingStatusUpdateRequest;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingJobResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingRegisterResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingStatusResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "Matching Command", description = "Matching registration and admin matching job APIs")
public interface MatchingCommandControllerDocs {

    @Operation(summary = "Register matching participant")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Registration succeeded"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Matching registration closed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate registration")
    })
    ResponseEntity<ApiResponse<MatchingRegisterResponse>> register(MatchingCreateRequest request);

    @Operation(summary = "Run matching job")
    ResponseEntity<ApiResponse<MatchingJobResponse>> runMatchingJob(@Parameter(hidden = true) AdminInfo admin);

    @Operation(summary = "Update matching service status")
    ResponseEntity<ApiResponse<MatchingStatusResponse>> updateStatus(
            @Parameter(hidden = true) AdminInfo admin,
            MatchingStatusUpdateRequest request
    );
}
