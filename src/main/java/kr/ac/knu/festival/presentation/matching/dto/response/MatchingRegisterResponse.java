package kr.ac.knu.festival.presentation.matching.dto.response;

import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;

import java.time.LocalDate;

public record MatchingRegisterResponse(
        String instagramId,
        LocalDate festivalDay,
        MatchingParticipantStatus status,
        String registrationDeadline,
        String resultOpenAt
) {
    public static MatchingRegisterResponse fromEntity(
            MatchingParticipant participant,
            String registrationDeadline,
            String resultOpenAt
    ) {
        return new MatchingRegisterResponse(
                participant.getInstagramId(),
                participant.getFestivalDay(),
                participant.getStatus(),
                registrationDeadline,
                resultOpenAt
        );
    }
}
