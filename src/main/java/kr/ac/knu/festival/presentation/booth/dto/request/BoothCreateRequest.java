package kr.ac.knu.festival.presentation.booth.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BoothCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal xRatio,
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal yRatio,
        @Size(max = 500) String menuBoardImageUrl,
        @NotBlank String adminPassword,
        @Size(max = 100) String department,
        @Size(max = 200) String location
) {}
