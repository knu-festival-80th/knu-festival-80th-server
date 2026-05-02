package kr.ac.knu.festival.application.matching;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class MatchingScheduleProperties {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final OffsetDateTime registrationDeadline;
    private final OffsetDateTime resultOpenAt;
    private final Clock clock;

    @Autowired
    public MatchingScheduleProperties(
            @Value("${matching.registration-deadline:2026-05-21T21:00:00+09:00}") OffsetDateTime registrationDeadline,
            @Value("${matching.result-open-at:2026-05-21T22:00:00+09:00}") OffsetDateTime resultOpenAt
    ) {
        this(registrationDeadline, resultOpenAt, Clock.system(KOREA_ZONE));
    }

    MatchingScheduleProperties(OffsetDateTime registrationDeadline, OffsetDateTime resultOpenAt, Clock clock) {
        this.registrationDeadline = registrationDeadline;
        this.resultOpenAt = resultOpenAt;
        this.clock = clock;
    }

    public boolean isRegistrationOpen() {
        return now().isBefore(registrationDeadline);
    }

    public boolean isRegistrationClosed() {
        return !isRegistrationOpen();
    }

    public boolean isResultOpen() {
        return !now().isBefore(resultOpenAt);
    }

    public OffsetDateTime registrationDeadline() {
        return registrationDeadline;
    }

    public OffsetDateTime resultOpenAt() {
        return resultOpenAt;
    }

    private OffsetDateTime now() {
        return ZonedDateTime.now(clock).toOffsetDateTime();
    }
}
