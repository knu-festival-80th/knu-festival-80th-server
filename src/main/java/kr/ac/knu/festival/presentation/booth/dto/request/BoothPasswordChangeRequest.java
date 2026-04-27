package kr.ac.knu.festival.presentation.booth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BoothPasswordChangeRequest(
        @NotBlank String newPassword
) {}
