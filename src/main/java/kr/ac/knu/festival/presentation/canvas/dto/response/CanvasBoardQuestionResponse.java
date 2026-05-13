package kr.ac.knu.festival.presentation.canvas.dto.response;

import kr.ac.knu.festival.domain.canvas.entity.CanvasBoardQuestion;

public record CanvasBoardQuestionResponse(
        Long questionId,
        String content,
        String description,
        int orderIndex
) {
    public static CanvasBoardQuestionResponse fromEntity(CanvasBoardQuestion entity) {
        return new CanvasBoardQuestionResponse(
                entity.getId(),
                entity.getContent(),
                entity.getDescription(),
                entity.getOrderIndex()
        );
    }
}
