package kr.ac.knu.festival.presentation.matching.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.ac.knu.festival.domain.matching.entity.MatchingOperationStatus;

public record MatchingStatusUpdateRequest(
        @NotNull
        MatchingOperationStatus status,

        @Size(max = 255)
        String messageKo,

        @Size(max = 255)
        String messageEn
) {
}
