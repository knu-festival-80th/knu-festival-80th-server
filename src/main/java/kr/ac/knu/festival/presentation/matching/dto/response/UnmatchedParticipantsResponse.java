package kr.ac.knu.festival.presentation.matching.dto.response;

import java.util.List;

public record UnmatchedParticipantsResponse(
        boolean resultOpen,
        String resultOpenAt,
        String messageKo,
        String messageEn,
        List<UnmatchedParticipantResponse> participants
) {
    public static UnmatchedParticipantsResponse hidden(String resultOpenAt) {
        return new UnmatchedParticipantsResponse(
                false,
                resultOpenAt,
                "미매칭 공개 목록은 결과 공개 시간 이후 확인할 수 있습니다.",
                "The unmatched list is available after the result open time.",
                List.of()
        );
    }

    public static UnmatchedParticipantsResponse open(
            String resultOpenAt,
            List<UnmatchedParticipantResponse> participants
    ) {
        return new UnmatchedParticipantsResponse(
                true,
                resultOpenAt,
                "미매칭 공개 목록이 공개되었습니다.",
                "The unmatched list is open.",
                participants
        );
    }
}
