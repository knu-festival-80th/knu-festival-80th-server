package kr.ac.knu.festival.presentation.booth.dto.response;

import kr.ac.knu.festival.domain.booth.entity.Booth;

import java.math.BigDecimal;

public record BoothMapResponse(
        Long boothId,
        String name,
        BigDecimal xRatio,
        BigDecimal yRatio
) {
    public static BoothMapResponse fromEntity(Booth booth) {
        return new BoothMapResponse(
                booth.getId(),
                booth.getName(),
                booth.getXRatio(),
                booth.getYRatio()
        );
    }
}
