package kr.ac.knu.festival.presentation.auth.dto.response;

public record LoginResponse(
        String role,
        Long boothId
) {}
