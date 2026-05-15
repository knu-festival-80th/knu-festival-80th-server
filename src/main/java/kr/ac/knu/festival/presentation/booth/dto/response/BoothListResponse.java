package kr.ac.knu.festival.presentation.booth.dto.response;

import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.infra.storage.ImageUrlResolver;

import java.math.BigDecimal;

public record BoothListResponse(
        Long boothId,
        String name,
        BigDecimal xRatio,
        BigDecimal yRatio,
        int likeCount,
        String menuBoardImageUrl,
        boolean waitingOpen,
        long currentWaitingTeams,
        String department,
        String location,
        String type
) {
    public static BoothListResponse fromEntity(Booth booth, long currentWaitingTeams, ImageUrlResolver urls) {
        return fromEntity(booth, currentWaitingTeams, booth.getLikeCount(), urls);
    }

    public static BoothListResponse fromEntity(
            Booth booth,
            long currentWaitingTeams,
            int likeCount,
            ImageUrlResolver urls
    ) {
        return new BoothListResponse(
                booth.getId(),
                booth.getName(),
                booth.getXRatio(),
                booth.getYRatio(),
                likeCount,
                urls.toPublicUrl(booth.getMenuBoardImageUrl()),
                booth.isWaitingOpen(),
                currentWaitingTeams,
                booth.getDepartment(),
                booth.getLocation(),
                booth.getMapLocationType() != null ? booth.getMapLocationType().name() : null
        );
    }
}
