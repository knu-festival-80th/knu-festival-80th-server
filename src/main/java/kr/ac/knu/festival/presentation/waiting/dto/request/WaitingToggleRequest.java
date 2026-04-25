package kr.ac.knu.festival.presentation.waiting.dto.request;

import jakarta.validation.constraints.NotNull;

public record WaitingToggleRequest(
        @NotNull Boolean open
) {}
