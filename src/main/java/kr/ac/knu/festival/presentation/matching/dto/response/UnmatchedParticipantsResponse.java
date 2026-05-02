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
        // 단순 403 대신 빈 목록과 공개 시간을 주면 프론트가 카운트다운/대기 화면을 만들기 쉽다.
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
        // 공개 여부와 목록을 한 응답에 담아, 22시 전후 프론트 분기 조건을 resultOpen 하나로 통일한다.
        return new UnmatchedParticipantsResponse(
                true,
                resultOpenAt,
                "미매칭 공개 목록이 공개되었습니다.",
                "The unmatched list is open.",
                participants
        );
    }
}
