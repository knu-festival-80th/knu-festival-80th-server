package kr.ac.knu.festival.presentation.canvas.dto.response;

import kr.ac.knu.festival.domain.canvas.entity.CanvasPostit;
import kr.ac.knu.festival.domain.canvas.entity.PostitColor;

import java.time.LocalDateTime;

public record CanvasPostitResponse(
        Long canvasPostitId,
        String nickname,
        String message,
        PostitColor color,
        int positionX,
        int positionY,
        int width,
        int height,
        LocalDateTime createdAt
) {
    public static CanvasPostitResponse fromEntity(CanvasPostit entity) {
        return new CanvasPostitResponse(
                entity.getId(),
                entity.getNickname(),
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