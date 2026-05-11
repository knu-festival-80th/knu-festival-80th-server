package kr.ac.knu.festival.presentation.matching.dto.response;

import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;

public record MatchingResultResponse(
        String instagramId,
        MatchingParticipantStatus status,
        boolean resultOpen,
        String matchedInstagramId,
        String instagramProfileUrl,
        String messageKo,
        String messageEn
) {
    public static MatchingResultResponse hidden(MatchingParticipant participant, String resultOpenAt) {
        return new MatchingResultResponse(
                participant.getInstagramId(),
                participant.getStatus(),
                false,
                null,
                null,
                "매칭 결과는 " + resultOpenAt + "에 공개됩니다.",
                "Matching results open at " + resultOpenAt + "."
        );
    }

    public static MatchingResultResponse fromEntity(MatchingParticipant participant) {
        String matchedId = participant.getMatchedId();
        return new MatchingResultResponse(
                participant.getInstagramId(),
                participant.getStatus(),
                true,
                matchedId,
                matchedId == null ? null : "https://instagram.com/" + matchedId,
                matchedId == null ? "아직 매칭된 상대가 없습니다." : "매칭 결과가 공개되었습니다.",
                matchedId == null ? "No matched participant is available yet." : "Matching result is open."
        );
    }
}
