package kr.ac.knu.festival.presentation.matching.dto.response;

import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;

public record MatchingResultResponse(
        String instagramId,
        MatchingParticipantStatus status,
        boolean resultOpen,
        String pickedInstagramId,
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
        String pickedId = participant.getMatchedId();
        return new MatchingResultResponse(
                participant.getInstagramId(),
                participant.getStatus(),
                true,
                pickedId,
                pickedId == null ? null : "https://instagram.com/" + pickedId,
                pickedId == null ? "매칭이 성사되지 않았습니다." : "당신이 뽑은 상대가 공개되었습니다.",
                pickedId == null ? "No partner was matched." : "Your picked partner is open."
        );
    }
}
