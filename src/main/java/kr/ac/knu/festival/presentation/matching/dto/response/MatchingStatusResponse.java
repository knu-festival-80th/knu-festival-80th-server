package kr.ac.knu.festival.presentation.matching.dto.response;

import kr.ac.knu.festival.domain.matching.entity.MatchingOperationStatus;
import kr.ac.knu.festival.domain.matching.entity.MatchingServiceState;

import java.time.LocalDate;
import java.util.List;

public record MatchingStatusResponse(
        MatchingOperationStatus status,
        boolean registrationOpen,
        boolean resultOpen,
        String registrationDeadline,
        String resultOpenAt,
        String registrationOpenAt,
        List<LocalDate> festivalDays,
        long pendingCount,
        long matchedCount,
        long unmatchedCount,
        long malePendingCount,
        long femalePendingCount
) {
    public static MatchingStatusResponse of(
            MatchingServiceState state,
            boolean registrationOpen,
            boolean resultOpen,
            String registrationDeadline,
            String resultOpenAt,
            String registrationOpenAt,
            List<LocalDate> festivalDays,
            long pendingCount,
            long matchedCount,
            long unmatchedCount,
            long malePendingCount,
            long femalePendingCount
    ) {
        return new MatchingStatusResponse(
                state.getStatus(),
                registrationOpen,
                resultOpen,
                registrationDeadline,
                resultOpenAt,
                registrationOpenAt,
                festivalDays,
                pendingCount,
                matchedCount,
                unmatchedCount,
                malePendingCount,
                femalePendingCount
        );
    }

    public static MatchingStatusResponse ofCached(
            MatchingOperationStatus status,
            boolean registrationOpen,
            boolean resultOpen,
            String registrationDeadline,
            String resultOpenAt,
            String registrationOpenAt,
            List<LocalDate> festivalDays,
            long pendingCount,
            long matchedCount,
            long unmatchedCount,
            long malePendingCount,
            long femalePendingCount
    ) {
        return new MatchingStatusResponse(
                status,
                registrationOpen,
                resultOpen,
                registrationDeadline,
                resultOpenAt,
                registrationOpenAt,
                festivalDays,
                pendingCount,
                matchedCount,
                unmatchedCount,
                malePendingCount,
                femalePendingCount
        );
    }
}
