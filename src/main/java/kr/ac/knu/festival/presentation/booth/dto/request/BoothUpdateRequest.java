package kr.ac.knu.festival.presentation.booth.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BoothUpdateRequest(
        @Size(max = 100) String name,
        String description,
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal xRatio,
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal yRatio,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String menuBoardImageUrl
) {}
