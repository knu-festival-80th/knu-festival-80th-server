package kr.ac.knu.festival.application.matching;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingSchedulePropertiesTest {

    private static final List<LocalDate> DAYS = List.of(
            LocalDate.parse("2026-05-20"),
            LocalDate.parse("2026-05-21"),
            LocalDate.parse("2026-05-22")
    );

    @Test
    void registrationOpenInsideWindow() {
        MatchingScheduleProperties props = at("2026-05-20T15:00:00+09:00");
        assertThat(props.isRegistrationOpen()).isTrue();
        assertThat(props.currentRegistrationDay()).contains(LocalDate.parse("2026-05-20"));
        assertThat(props.isResultOpen()).isFalse();
    }

    @Test
    void registrationClosedBeforeLowerBound() {
        MatchingScheduleProperties props = at("2026-05-20T10:59:00+09:00");
        assertThat(props.isRegistrationOpen()).isFalse();
        assertThat(props.currentRegistrationDay()).isEmpty();
    }

    @Test
    void registrationClosedAtCloseTime() {
        MatchingScheduleProperties props = at("2026-05-20T21:00:00+09:00");
        assertThat(props.isRegistrationOpen()).isFalse();
        assertThat(props.dayPendingMatching()).contains(LocalDate.parse("2026-05-20"));
    }

    @Test
    void resultOpenAfter22() {
        MatchingScheduleProperties props = at("2026-05-20T22:30:00+09:00");
        assertThat(props.isResultOpen()).isTrue();
        assertThat(props.currentResultDay()).contains(LocalDate.parse("2026-05-20"));
    }

    @Test
    void resultStillOpenNextMorningBefore11() {
        MatchingScheduleProperties props = at("2026-05-21T10:59:00+09:00");
        assertThat(props.isResultOpen()).isTrue();
        assertThat(props.currentResultDay()).contains(LocalDate.parse("2026-05-20"));
    }

    @Test
    void resultClosedAtNextMorning11() {
        MatchingScheduleProperties props = at("2026-05-21T11:00:00+09:00");
        assertThat(props.isResultOpen()).isFalse();
        assertThat(props.currentResultDay()).isEmpty();
        assertThat(props.currentRegistrationDay()).contains(LocalDate.parse("2026-05-21"));
    }

    @Test
    void noActivityOutsideFestival() {
        MatchingScheduleProperties props = at("2026-05-23T15:00:00+09:00");
        assertThat(props.isRegistrationOpen()).isFalse();
        assertThat(props.isResultOpen()).isFalse();
        assertThat(props.dayPendingMatching()).isEmpty();
    }

    @Test
    void deadZoneBetween21And22() {
        MatchingScheduleProperties props = at("2026-05-21T21:30:00+09:00");
        assertThat(props.isRegistrationOpen()).isFalse();
        assertThat(props.isResultOpen()).isFalse();
        assertThat(props.dayPendingMatching()).contains(LocalDate.parse("2026-05-21"));
    }

    private MatchingScheduleProperties at(String iso) {
        Clock clock = Clock.fixed(
                Instant.parse(OffsetDateTime.parse(iso).toInstant().toString()),
                ZoneId.of("Asia/Seoul")
        );
        return new MatchingScheduleProperties(
                DAYS,
                LocalTime.of(11, 0),
                LocalTime.of(21, 0),
                LocalTime.of(22, 0),
                LocalTime.of(11, 0),
                clock
        );
    }
}
