package kr.ac.knu.festival.presentation.waiting.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WaitingInsertRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull @Min(1) Integer partySize,
        @NotBlank
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "올바른 휴대전화 번호 형식이 아닙니다.")
        String phoneNumber,
        @NotNull @Min(1) Integer insertAfterSortOrder
) {}
