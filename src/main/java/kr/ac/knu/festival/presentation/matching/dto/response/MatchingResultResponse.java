package kr.ac.knu.festival.presentation.matching.dto.response;

import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;

public record MatchingResultResponse(
        String instagramId,
        MatchingParticipantStatus status,
        boolean resultOpen,
        String pickedInstagramId,
        String instagramProfileUrl,
        String resultOpenAt
) {
    public static MatchingResultResponse hidden(MatchingParticipant participant, String resultOpenAt) {
        return new MatchingResultResponse(
                participant.getInstagramId(),
                participant.getStatus(),
                false,
                null,
                null,
                resultOpenAt
        );
    }

    public static MatchingResultResponse fromEntity(MatchingParticipant participant) {
        String pickedId = participant.getMatchedId();
        return new MatchingResultResponse(
                participant.getInstagramId(),
                participant.getStatus(),
                true,
                pickedId,
                pickedId == null ? null : "https://instagram.com/" + pickedId,
                null
        );
    }
}
