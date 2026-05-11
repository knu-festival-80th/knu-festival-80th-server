package kr.ac.knu.festival.presentation.canvas.dto.response;

import kr.ac.knu.festival.domain.canvas.entity.CanvasPostit;
import kr.ac.knu.festival.domain.canvas.entity.PostitColor;

import java.time.LocalDateTime;

public record CanvasPostitCreateResponse(
        Long canvasPostitId,
        int zoneNumber,
        String message,
        PostitColor color,
        int positionX,
        int positionY,
        int width,
        int height,
        LocalDateTime createdAt
) {
    public static CanvasPostitCreateResponse fromEntity(CanvasPostit entity) {
        return new CanvasPostitCreateResponse(
                entity.getId(),
                entity.getZoneNumber(),
                entity.getMessage(),
                entity.getColor(),
                entity.getPositionX(),
                entity.getPositionY(),
                entity.getWidth(),
                entity.getHeight(),
                entity.getCreatedAt()
        );
    }
}