package kr.ac.knu.festival.presentation.waiting.dto.response;

public record WaitingStatusResponse(
        Long boothId,
        boolean waitingOpen,
        long currentWaitingTeams,
        int estimatedWaitMinutes
) {
    public static WaitingStatusResponse of(
            Long boothId,
            boolean waitingOpen,
            long currentWaitingTeams,
            int estimatedWaitMinutes
    ) {
        return new WaitingStatusResponse(boothId, waitingOpen, currentWaitingTeams, estimatedWaitMinutes);
    }
}
