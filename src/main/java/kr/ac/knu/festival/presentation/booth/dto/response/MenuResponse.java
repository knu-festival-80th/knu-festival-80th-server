package kr.ac.knu.festival.presentation.booth.dto.response;

import kr.ac.knu.festival.domain.booth.entity.Menu;

public record MenuResponse(
        Long menuId,
        String name,
        int price,
        String imageUrl,
        String description,
        boolean soldOut
) {
    public static MenuResponse fromEntity(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getPrice(),
                menu.getImageUrl(),
                menu.getDescription(),
                menu.isSoldOut()
        );
    }
}
