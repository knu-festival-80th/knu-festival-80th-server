package kr.ac.knu.festival.presentation.canvas.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CanvasPostitCreateRequest(
        @NotNull
        @Positive
        Long boardId,

        @NotNull
        @Min(value = 1, message = "colorId는 1~6 사이여야 합니다.")
        @Max(value = 6, message = "colorId는 1~6 사이여야 합니다.")
        Integer colorId,

        @NotBlank
        @Size(max = 60, message = "메시지는 최대 60자입니다.")
        String message,

        @NotNull
        @Valid
        Placement placement
) {
    public record Placement(
            @NotNull
            @DecimalMin(value = "0.0", message = "x는 0 이상이어야 합니다.")
            @DecimalMax(value = "100.0", message = "x는 100 이하여야 합니다.")
            Double x,

            @NotNull
            @DecimalMin(value = "0.0", message = "y는 0 이상이어야 합니다.")
            @DecimalMax(value = "100.0", message = "y는 100 이하여야 합니다.")
            Double y
    ) {}
}
