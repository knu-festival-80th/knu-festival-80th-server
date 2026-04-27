package kr.ac.knu.festival.presentation.booth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BoothCreateRequest(
        @NotBlank @Size(max = 100) String name,
        String description,
        BigDecimal locationLat,
        BigDecimal locationLng,
        @Size(max = 500) String imageUrl,
        @NotBlank String adminPassword
) {}
