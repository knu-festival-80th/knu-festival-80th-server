package kr.ac.knu.festival.presentation.canvas.dto.response;

public record BoardSummaryResponse(
        Long boardId,
        Long questionId,
        int boardVariant,
        long noteCount,
        int maxNoteCount
) {}
