package kr.ac.knu.festival.presentation.matching.dto.response;

public record MatchingJobResponse(
        int matchedPairCount,
        int unmatchedCount
) {
}
