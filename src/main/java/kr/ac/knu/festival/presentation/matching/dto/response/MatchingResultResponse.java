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
    public static MatchingResultResponse hidden(MatchingParticipant participant) {
        // 공개 전 응답도 동일한 DTO를 쓰면 프론트가 잠금/공개 화면을 같은 API로 처리할 수 있다.
        return new MatchingResultResponse(
                participant.getInstagramId(),
                participant.getStatus(),
                false,
                null,
                null,
                "매칭 결과는 2026년 5월 21일 22시에 공개됩니다.",
                "Matching results open at 22:00 on May 21, 2026."
        );
    }

    public static MatchingResultResponse fromEntity(MatchingParticipant participant) {
        String matchedId = participant.getMatchedId();
        // Instagram 딥링크는 프론트가 별도 규칙을 중복 구현하지 않도록 서버에서 함께 내려준다.
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
