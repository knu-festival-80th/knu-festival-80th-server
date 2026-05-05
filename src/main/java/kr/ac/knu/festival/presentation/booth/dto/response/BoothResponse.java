package kr.ac.knu.festival.presentation.booth.dto.response;

import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.infra.storage.ImageUrlResolver;

import java.math.BigDecimal;

public record BoothResponse(
        Long boothId,
        String name,
        String description,
        BigDecimal xRatio,
        BigDecimal yRatio,
        int likeCount,
        String imageUrl,
        String menuBoardImageUrl,
        boolean waitingOpen
) {
    public static BoothResponse fromEntity(Booth booth, ImageUrlResolver urls) {
        return new BoothResponse(
                booth.getId(),
                booth.getName(),
                booth.getDescription(),
                booth.getXRatio(),
                booth.getYRatio(),
                booth.getLikeCount(),
                urls.toPublicUrl(booth.getImageUrl()),
                urls.toPublicUrl(booth.getMenuBoardImageUrl()),
                booth.isWaitingOpen()
        );
    }
}
