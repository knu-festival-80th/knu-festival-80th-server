package kr.ac.knu.festival.presentation.booth.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record MenuUpdateRequest(
        @Size(max = 100) String name,
        @Min(0) Integer price,
        @Size(max = 500) String imageUrl,
        String description
) {}
