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
        boolean waitingOpen,
        String department,
        String category,
        String location
) {
    public static BoothResponse fromEntity(Booth booth, ImageUrlResolver urls) {
        return fromEntity(booth, booth.getLikeCount(), urls);
    }

    public static BoothResponse fromEntity(Booth booth, int likeCount, ImageUrlResolver urls) {
        return new BoothResponse(
                booth.getId(),
                booth.getName(),
                booth.getDescription(),
                booth.getXRatio(),
                booth.getYRatio(),
                likeCount,
                urls.toPublicUrl(booth.getImageUrl()),
                urls.toPublicUrl(booth.getMenuBoardImageUrl()),
                booth.isWaitingOpen(),
                booth.getDepartment(),
                booth.getCategory(),
                booth.getLocation()
        );
    }
}
