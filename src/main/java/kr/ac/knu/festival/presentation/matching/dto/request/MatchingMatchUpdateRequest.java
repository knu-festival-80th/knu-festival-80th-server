package kr.ac.knu.festival.presentation.matching.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MatchingMatchUpdateRequest(
        @NotBlank
        @Size(max = 100)
        String matchedInstagramId
) {
}
