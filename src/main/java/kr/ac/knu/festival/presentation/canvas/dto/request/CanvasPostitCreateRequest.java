package kr.ac.knu.festival.presentation.canvas.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.ac.knu.festival.domain.canvas.entity.PostitColor;

public record CanvasPostitCreateRequest(
        @NotBlank
        @Size(max = 60, message = "메시지는 최대 60자입니다.")
        String message,

        @NotNull
        PostitColor color,

        @NotNull
        @Min(value = 0, message = "positionX는 0 이상이어야 합니다.")
        Integer positionX,

        @NotNull
        @Min(value = 0, message = "positionY는 0 이상이어야 합니다.")
        Integer positionY,

        @NotNull
        Integer width,

        @NotNull
        Integer height
) {
}