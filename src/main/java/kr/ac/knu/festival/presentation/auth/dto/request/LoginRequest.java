package kr.ac.knu.festival.presentation.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        Long boothId,
        @NotBlank String password
) {}
