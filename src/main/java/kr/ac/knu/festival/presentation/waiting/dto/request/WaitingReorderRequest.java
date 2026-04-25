package kr.ac.knu.festival.presentation.waiting.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WaitingReorderRequest(
        @NotNull @Min(1) Integer newSortOrder
) {}
