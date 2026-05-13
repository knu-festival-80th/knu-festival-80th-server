package kr.ac.knu.festival.presentation.canvas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CanvasBoardCreateRequest(
        @NotNull
        Long questionId,

        @NotNull
        @Min(value = 1, message = "maxNoteCount는 1 이상이어야 합니다.")
        Integer maxNoteCount
) {}
