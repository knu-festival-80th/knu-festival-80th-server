package kr.ac.knu.festival.presentation.booth.dto.response;

import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.domain.booth.entity.Menu;

import java.math.BigDecimal;
import java.util.List;

public record BoothDetailResponse(
        Long boothId,
        String name,
        String description,
        BigDecimal xRatio,
        BigDecimal yRatio,
        int likeCount,
        String imageUrl,
        String menuBoardImageUrl,
        boolean waitingOpen,
        long currentWaitingTeams,
        List<MenuResponse> menus
) {
    public static BoothDetailResponse of(Booth booth, List<Menu> menus, long currentWaitingTeams) {
        return new BoothDetailResponse(
                booth.getId(),
                booth.getName(),
                booth.getDescription(),
                booth.getXRatio(),
                booth.getYRatio(),
                booth.getLikeCount(),
                booth.getImageUrl(),
                booth.getMenuBoardImageUrl(),
                booth.isWaitingOpen(),
                currentWaitingTeams,
                menus.stream().map(MenuResponse::fromEntity).toList()
        );
    }
}
