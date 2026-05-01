package kr.ac.knu.festival.presentation.matching.dto.response;

import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;

public record MatchingResultResponse(
        String instagramId,
        MatchingParticipantStatus status,
        String matchedInstagramId,
        String instagramProfileUrl
) {
    public static MatchingResultResponse fromEntity(MatchingParticipant participant) {
        String matchedId = participant.getMatchedId();
        return new MatchingResultResponse(
                participant.getInstagramId(),
                participant.getStatus(),
                matchedId,
                matchedId == null ? null : "https://instagram.com/" + matchedId
        );
    }
}
