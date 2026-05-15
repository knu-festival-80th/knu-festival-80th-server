package kr.ac.knu.festival.presentation.booth.dto.response;

import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.infra.storage.ImageUrlResolver;

import java.math.BigDecimal;

public record BoothResponse(
        Long boothId,
        String name,
        BigDecimal xRatio,
        BigDecimal yRatio,
        int likeCount,
        int totalWaitingCount,
        String menuBoardImageUrl,
        boolean waitingOpen,
        String department,
        String location
) {
    public static BoothResponse fromEntity(Booth booth, ImageUrlResolver urls) {
        return fromEntity(booth, booth.getLikeCount(), booth.getTotalWaitingCount(), urls);
    }

    public static BoothResponse fromEntity(Booth booth, int likeCount, ImageUrlResolver urls) {
        return fromEntity(booth, likeCount, booth.getTotalWaitingCount(), urls);
    }

    public static BoothResponse fromEntity(
            Booth booth,
            int likeCount,
            int totalWaitingCount,
            ImageUrlResolver urls
    ) {
        return new BoothResponse(
                booth.getId(),
                booth.getName(),
                booth.getXRatio(),
                booth.getYRatio(),
                likeCount,
                totalWaitingCount,
                urls.toPublicUrl(booth.getMenuBoardImageUrl()),
                booth.isWaitingOpen(),
                booth.getDepartment(),
                booth.getLocation()
        );
    }
}
