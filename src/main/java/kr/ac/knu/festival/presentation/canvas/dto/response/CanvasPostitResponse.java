package kr.ac.knu.festival.presentation.canvas.dto.response;

import kr.ac.knu.festival.domain.canvas.entity.CanvasPostit;

import java.time.LocalDateTime;

public record CanvasPostitResponse(
        Long canvasPostitId,
        Long boardId,
        int boardVariant,
        int colorId,
        String message,
        Placement placement,
        LocalDateTime createdAt
) {
    public record Placement(double x, double y) {}

    public static CanvasPostitResponse fromEntity(CanvasPostit entity) {
        return new CanvasPostitResponse(
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
