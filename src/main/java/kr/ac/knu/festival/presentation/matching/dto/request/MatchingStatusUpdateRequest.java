package kr.ac.knu.festival.presentation.matching.dto.request;

import jakarta.validation.constraints.NotNull;
import kr.ac.knu.festival.domain.matching.entity.MatchingOperationStatus;

public record MatchingStatusUpdateRequest(
        @NotNull
        MatchingOperationStatus status
) {
}
