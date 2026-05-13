package kr.ac.knu.festival.presentation.canvas.dto.response;

import kr.ac.knu.festival.domain.canvas.entity.CanvasPostit;

import java.time.LocalDateTime;

public record CanvasPostitCreateResponse(
        Long canvasPostitId,
        Long boardId,
        int boardVariant,
        int colorId,
        String message,
        Placement placement,
        LocalDateTime createdAt
) {
    public record Placement(double x, double y) {}

    public static CanvasPostitCreateResponse fromEntity(CanvasPostit entity) {
        return new CanvasPostitCreateResponse(
                entity.getId(),
                entity.getBoard().getId(),
                entity.getBoard().getQuestion().getBoardVariant(),
                entity.getColorId(),
                entity.getMessage(),
                new Placement(entity.getPositionX(), entity.getPositionY()),
                entity.getCreatedAt()
        );
    }
}
