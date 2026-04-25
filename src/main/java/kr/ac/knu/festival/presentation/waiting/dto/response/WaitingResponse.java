package kr.ac.knu.festival.presentation.waiting.dto.response;

import kr.ac.knu.festival.domain.waiting.entity.Waiting;

import java.time.LocalDateTime;

public record WaitingResponse(
        Long waitingId,
        Long boothId,
        int waitingNumber,
        int sortOrder,
        String name,
        int partySize,
        String maskedPhoneNumber,
        String status,
        boolean smsSent,
        LocalDateTime calledAt,
        LocalDateTime enteredAt,
        LocalDateTime createdAt
) {
    public static WaitingResponse fromEntity(Waiting waiting, String maskedPhoneNumber) {
        return new WaitingResponse(
                waiting.getId(),
                waiting.getBooth().getId(),
                waiting.getWaitingNumber(),
                waiting.getSortOrder(),
                waiting.getName(),
                waiting.getPartySize(),
                maskedPhoneNumber,
                waiting.getStatus().name(),
                waiting.isSmsSent(),
                waiting.getCalledAt(),
                waiting.getEnteredAt(),
                waiting.getCreatedAt()
        );
    }
}
