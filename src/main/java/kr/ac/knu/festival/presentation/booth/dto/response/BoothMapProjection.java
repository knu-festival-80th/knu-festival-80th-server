package kr.ac.knu.festival.presentation.booth.dto.response;

import kr.ac.knu.festival.domain.booth.entity.MapLocationType;

import java.math.BigDecimal;

public record BoothMapProjection(Long id, String name, BigDecimal xRatio, BigDecimal yRatio, MapLocationType type) {
}
