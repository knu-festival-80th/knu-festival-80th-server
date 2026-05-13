package kr.ac.knu.festival.application.matching;

import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingJobResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchingAutoSchedulerTest {

    private static final List<LocalDate> FESTIVAL_DAYS = List.of(
            LocalDate.parse("2026-05-20"),
            LocalDate.parse("2026-05-21"),
            LocalDate.parse("2026-05-22")
    );
    private static final LocalDate DAY_20 = LocalDate.parse("2026-05-20");

    private final MatchingParticipantRepository matchingParticipantRepository = mock(MatchingParticipantRepository.class);
    private final MatchingCommandService matchingCommandService = mock(MatchingCommandService.class);

    @Test
    void doNotRunBeforeRegistrationDeadline() {
        MatchingAutoScheduler scheduler = schedulerAt("2026-05-20T20:59:00+09:00");

        scheduler.runDuringMatchingWindow();

        verify(matchingParticipantRepository, never())
                .countByFestivalDayAndStatus(any(), any());
        verify(matchingCommandService, never()).runMatchingJobFor(any());
    }

    @Test
    void runDuringMatchingWindowWhenPendingExist() {
        MatchingAutoScheduler scheduler = schedulerAt("2026-05-20T21:00:00+09:00");
        when(matchingParticipantRepository.countByFestivalDayAndStatus(DAY_20, MatchingParticipantStatus.PENDING))
                .thenReturn(4L);
        when(matchingCommandService.runMatchingJobFor(DAY_20))
                .thenReturn(new MatchingJobResponse(2, 0));

        scheduler.runDuringMatchingWindow();

        verify(matchingParticipantRepository).countByFestivalDayAndStatus(DAY_20, MatchingParticipantStatus.PENDING);
        verify(matchingCommandService).runMatchingJobFor(DAY_20);
    }

    @Test
    void doNotRunWhenNoPendingRemain() {
        MatchingAutoScheduler scheduler = schedulerAt("2026-05-20T21:30:00+09:00");
        when(matchingParticipantRepository.countByFestivalDayAndStatus(DAY_20, MatchingParticipantStatus.PENDING))
                .thenReturn(0L);

        scheduler.runDuringMatchingWindow();

        verify(matchingParticipantRepository).countByFestivalDayAndStatus(DAY_20, MatchingParticipantStatus.PENDING);
        verify(matchingCommandService, never()).runMatchingJobFor(any());
    }

    @Test
    void doNotRunOnceResultWindowIsOpen() {
        MatchingAutoScheduler scheduler = schedulerAt("2026-05-20T22:00:00+09:00");

        scheduler.runDuringMatchingWindow();

        verify(matchingParticipantRepository, never()).countByFestivalDayAndStatus(any(), any());
        verify(matchingCommandService, never()).runMatchingJobFor(any());
    }

    private MatchingAutoScheduler schedulerAt(String now) {
        MatchingScheduleProperties scheduleProperties = new MatchingScheduleProperties(
                FESTIVAL_DAYS,
                LocalTime.of(11, 0),
                LocalTime.of(21, 0),
                LocalTime.of(22, 0),
                LocalTime.of(11, 0),
                Clock.fixed(Instant.parse(OffsetDateTime.parse(now).toInstant().toString()), ZoneId.of("Asia/Seoul"))
        );
        return new MatchingAutoScheduler(scheduleProperties, matchingParticipantRepository, matchingCommandService);
    }
}
