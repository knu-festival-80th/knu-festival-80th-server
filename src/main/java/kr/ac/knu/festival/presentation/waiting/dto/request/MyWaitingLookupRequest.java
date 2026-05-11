package kr.ac.knu.festival.presentation.waiting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MyWaitingLookupRequest(
        @NotBlank @Size(max = 50)
        String name,

        @NotBlank
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
        String phoneNumber
) {
}
