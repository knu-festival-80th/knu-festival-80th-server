package kr.ac.knu.festival.presentation.matching.dto.response;

import kr.ac.knu.festival.domain.matching.entity.MatchingOperationStatus;
import kr.ac.knu.festival.domain.matching.entity.MatchingServiceState;

public record MatchingStatusResponse(
        MatchingOperationStatus status,
        String messageKo,
        String messageEn,
        boolean registrationOpen,
        boolean resultOpen,
        String registrationDeadline,
        String resultOpenAt,
        long pendingCount,
        long matchedCount,
        long unmatchedCount
) {
    public static MatchingStatusResponse of(
            MatchingServiceState state,
            boolean registrationOpen,
            boolean resultOpen,
            String registrationDeadline,
            String resultOpenAt,
            long pendingCount,
            long matchedCount,
            long unmatchedCount
    ) {
        return new MatchingStatusResponse(
                state.getStatus(),
                state.getMessageKo(),
                state.getMessageEn(),
                registrationOpen,
                resultOpen,
                registrationDeadline,
                resultOpenAt,
                pendingCount,
                matchedCount,
                unmatchedCount
        );
    }

    public static MatchingStatusResponse ofCached(
            MatchingOperationStatus status,
            String messageKo,
            String messageEn,
            boolean registrationOpen,
            boolean resultOpen,
            String registrationDeadline,
            String resultOpenAt,
            long pendingCount,
            long matchedCount,
            long unmatchedCount
    ) {
        return new MatchingStatusResponse(
                status,
                messageKo,
                messageEn,
                registrationOpen,
                resultOpen,
                registrationDeadline,
                resultOpenAt,
                pendingCount,
                matchedCount,
                unmatchedCount
        );
    }
}
