package kr.ac.knu.festival.presentation.matching.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kr.ac.knu.festival.domain.matching.entity.MatchingGender;

public record MatchingCreateRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z0-9._]{1,100}$", message = "Instagram ID 형식이 올바르지 않습니다.")
        String instagramId,

        @NotNull
        MatchingGender gender,

        @NotBlank
        @Pattern(regexp = "^01[0-9]{8,9}$", message = "연락처 형식이 올바르지 않습니다.")
        String phoneNumber
) {
}
