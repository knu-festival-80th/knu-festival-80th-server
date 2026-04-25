package kr.ac.knu.festival.presentation.waiting.dto.response;

import kr.ac.knu.festival.domain.waiting.entity.Waiting;

public record MyWaitingResponse(
        Long waitingId,
        Long boothId,
        String boothName,
        int waitingNumber,
        String status,
        int aheadCount,
        int estimatedWaitMinutes
) {
    public static MyWaitingResponse of(Waiting waiting, int aheadCount, int estimatedWaitMinutes) {
        return new MyWaitingResponse(
                waiting.getId(),
                waiting.getBooth().getId(),
                waiting.getBooth().getName(),
                waiting.getWaitingNumber(),
                waiting.getStatus().name(),
                aheadCount,
                estimatedWaitMinutes
        );
    }
}
