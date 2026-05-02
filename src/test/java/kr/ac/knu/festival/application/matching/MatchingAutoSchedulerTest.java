package kr.ac.knu.festival.application.matching;

import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingJobResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchingAutoSchedulerTest {

    private static final OffsetDateTime DEADLINE = OffsetDateTime.parse("2026-05-21T21:00:00+09:00");
    private static final OffsetDateTime RESULT_OPEN = OffsetDateTime.parse("2026-05-21T22:00:00+09:00");

    private final MatchingParticipantRepository matchingParticipantRepository = mock(MatchingParticipantRepository.class);
    private final MatchingCommandService matchingCommandService = mock(MatchingCommandService.class);

    @Test
    void doNotRunBeforeRegistrationDeadline() {
        MatchingAutoScheduler scheduler = schedulerAt("2026-05-21T20:59:00+09:00");

        scheduler.runAfterRegistrationDeadline();

        verify(matchingParticipantRepository, never()).countByStatus(MatchingParticipantStatus.PENDING);
        verify(matchingCommandService, never()).runMatchingJob();
    }

    @Test
    void runWhenRegistrationClosedAndPendingParticipantsExist() {
        MatchingAutoScheduler scheduler = schedulerAt("2026-05-21T21:00:00+09:00");
        when(matchingParticipantRepository.countByStatus(MatchingParticipantStatus.PENDING)).thenReturn(2L);
        when(matchingCommandService.runMatchingJob()).thenReturn(new MatchingJobResponse(1, 0));

        scheduler.runAfterRegistrationDeadline();

        verify(matchingParticipantRepository).countByStatus(MatchingParticipantStatus.PENDING);
        verify(matchingCommandService).runMatchingJob();
    }

    @Test
    void doNotRunWhenNoPendingParticipantsRemain() {
        MatchingAutoScheduler scheduler = schedulerAt("2026-05-21T21:30:00+09:00");
        when(matchingParticipantRepository.countByStatus(MatchingParticipantStatus.PENDING)).thenReturn(0L);

        scheduler.runAfterRegistrationDeadline();

        verify(matchingParticipantRepository).countByStatus(MatchingParticipantStatus.PENDING);
        verify(matchingCommandService, never()).runMatchingJob();
    }

    private MatchingAutoScheduler schedulerAt(String now) {
        MatchingScheduleProperties scheduleProperties = new MatchingScheduleProperties(
                DEADLINE,
                RESULT_OPEN,
                Clock.fixed(Instant.parse(OffsetDateTime.parse(now).toInstant().toString()), ZoneId.of("Asia/Seoul"))
        );
        return new MatchingAutoScheduler(scheduleProperties, matchingParticipantRepository, matchingCommandService);
    }
}
