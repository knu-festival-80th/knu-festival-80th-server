package kr.ac.knu.festival.presentation.matching.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MatchingAuthRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z0-9._]{1,100}$", message = "Instagram ID 형식이 올바르지 않습니다.")
        String instagramId,

        @NotBlank
        @Size(min = 4, max = 100)
        String password
) {
}
