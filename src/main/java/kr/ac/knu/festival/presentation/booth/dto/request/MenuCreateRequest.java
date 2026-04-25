package kr.ac.knu.festival.presentation.booth.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MenuCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(0) Integer price,
        @Size(max = 500) String imageUrl,
        String description
) {}
