package kr.ac.knu.festival.presentation.booth.dto.response;

import kr.ac.knu.festival.domain.booth.entity.Booth;

import java.math.BigDecimal;

public record BoothListResponse(
        Long boothId,
        String name,
        String description,
        BigDecimal xRatio,
        BigDecimal yRatio,
        int likeCount,
        String imageUrl,
        String menuBoardImageUrl,
        boolean waitingOpen,
        long currentWaitingTeams
) {
    public static BoothListResponse fromEntity(Booth booth, long currentWaitingTeams) {
        return new BoothListResponse(
                booth.getId(),
                booth.getName(),
                booth.getDescription(),
                booth.getXRatio(),
                booth.getYRatio(),
                booth.getLikeCount(),
                booth.getImageUrl(),
                booth.getMenuBoardImageUrl(),
                booth.isWaitingOpen(),
                currentWaitingTeams
        );
    }
}
