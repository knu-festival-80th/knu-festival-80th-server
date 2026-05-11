package kr.ac.knu.festival.presentation.booth.dto.response;

import kr.ac.knu.festival.domain.booth.entity.Menu;
import kr.ac.knu.festival.infra.storage.ImageUrlResolver;

public record MenuResponse(
        Long menuId,
        String name,
        int price,
        String imageUrl,
        String description,
        boolean soldOut
) {
    public static MenuResponse fromEntity(Menu menu, ImageUrlResolver urls) {
        return new MenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getPrice(),
                urls.toPublicUrl(menu.getImageUrl()),
                menu.getDescription(),
                menu.isSoldOut()
        );
    }
}
