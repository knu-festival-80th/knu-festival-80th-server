package kr.ac.knu.festival.presentation.matching.dto.response;

import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;

public record UnmatchedParticipantResponse(
        String instagramId,
        MatchingGender gender,
        String instagramProfileUrl
) {
    public static UnmatchedParticipantResponse fromEntity(MatchingParticipant participant) {
        return new UnmatchedParticipantResponse(
                participant.getInstagramId(),
                participant.getGender(),
                "https://instagram.com/" + participant.getInstagramId()
        );
    }
}
