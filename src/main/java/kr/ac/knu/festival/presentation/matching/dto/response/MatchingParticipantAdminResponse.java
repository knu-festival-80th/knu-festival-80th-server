package kr.ac.knu.festival.presentation.matching.dto.response;

import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MatchingParticipantAdminResponse(
        Long participantId,
        String instagramId,
        MatchingGender gender,
        MatchingParticipantStatus status,
        String matchedInstagramId,
        LocalDate festivalDay,
        String maskedPhone,
        LocalDateTime createdAt
) {
    public static MatchingParticipantAdminResponse fromEntity(
            MatchingParticipant participant,
            String maskedPhone
    ) {
        return new MatchingParticipantAdminResponse(
                participant.getId(),
                participant.getInstagramId(),
                participant.getGender(),
                participant.getStatus(),
                participant.getMatchedId(),
                participant.getFestivalDay(),
                maskedPhone,
                participant.getCreatedAt()
        );
    }
}
