package kr.ac.knu.festival.presentation.matching.dto.response;

import java.time.LocalDate;

public record MatchingApplicantsCountResponse(
        LocalDate festivalDay,
        long malePendingCount,
        long femalePendingCount,
        long totalPendingCount
) {
    public static MatchingApplicantsCountResponse empty() {
        return new MatchingApplicantsCountResponse(null, 0, 0, 0);
    }
}
