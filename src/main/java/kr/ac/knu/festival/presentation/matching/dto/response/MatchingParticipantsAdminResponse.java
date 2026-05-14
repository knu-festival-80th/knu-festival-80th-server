package kr.ac.knu.festival.presentation.matching.dto.response;

import java.time.LocalDate;
import java.util.List;

public record MatchingParticipantsAdminResponse(
        LocalDate festivalDay,
        int totalCount,
        List<MatchingParticipantAdminResponse> participants
) {
    public static MatchingParticipantsAdminResponse of(
            LocalDate festivalDay,
            List<MatchingParticipantAdminResponse> participants
    ) {
        return new MatchingParticipantsAdminResponse(festivalDay, participants.size(), participants);
    }
}
