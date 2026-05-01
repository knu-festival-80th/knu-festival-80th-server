package kr.ac.knu.festival.presentation.matching.dto.response;

import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;

public record MatchingRegisterResponse(
        String instagramId,
        MatchingParticipantStatus status
) {
    public static MatchingRegisterResponse fromEntity(MatchingParticipant participant) {
        return new MatchingRegisterResponse(participant.getInstagramId(), participant.getStatus());
    }
}
