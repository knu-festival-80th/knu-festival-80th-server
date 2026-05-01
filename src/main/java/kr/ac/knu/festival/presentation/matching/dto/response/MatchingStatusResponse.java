package kr.ac.knu.festival.presentation.matching.dto.response;

import kr.ac.knu.festival.domain.matching.entity.MatchingOperationStatus;
import kr.ac.knu.festival.domain.matching.entity.MatchingServiceState;

public record MatchingStatusResponse(
        MatchingOperationStatus status,
        String messageKo,
        String messageEn
) {
    public static MatchingStatusResponse fromEntity(MatchingServiceState state) {
        return new MatchingStatusResponse(state.getStatus(), state.getMessageKo(), state.getMessageEn());
    }
}
