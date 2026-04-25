package kr.ac.knu.festival.presentation.waiting.dto.response;

import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.domain.waiting.entity.Waiting;

public record WaitingRegisterResponse(
        Long waitingId,
        int waitingNumber,
        String boothName,
        long currentWaitingTeams,
        int estimatedWaitMinutes
) {
    public static WaitingRegisterResponse of(
            Waiting waiting,
            Booth booth,
            long currentWaitingTeams,
            int estimatedWaitMinutes
    ) {
        return new WaitingRegisterResponse(
                waiting.getId(),
                waiting.getWaitingNumber(),
                booth.getName(),
                currentWaitingTeams,
                estimatedWaitMinutes
        );
    }
}
