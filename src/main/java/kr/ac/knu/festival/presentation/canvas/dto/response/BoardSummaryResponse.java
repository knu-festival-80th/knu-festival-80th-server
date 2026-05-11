package kr.ac.knu.festival.presentation.canvas.dto.response;

public record BoardSummaryResponse(
        Long boardId,
        int boardVariant,
        long noteCount
) {}
