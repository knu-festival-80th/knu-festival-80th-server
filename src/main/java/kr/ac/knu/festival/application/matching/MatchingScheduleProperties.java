package kr.ac.knu.festival.application.matching;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class MatchingScheduleProperties {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final List<LocalDate> festivalDays;
    private final LocalTime registrationOpenTime;
    private final LocalTime registrationCloseTime;
    private final LocalTime resultOpenTime;
    private final LocalTime resultCloseTime;
    private final Clock clock;

    @Autowired
    public MatchingScheduleProperties(
            @Value("${matching.festival-days:2026-05-20,2026-05-21,2026-05-22}") String festivalDaysCsv,
            @Value("${matching.registration-open-hour:11}") int registrationOpenHour,
            @Value("${matching.registration-close-hour:21}") int registrationCloseHour,
            @Value("${matching.result-open-hour:22}") int resultOpenHour,
            @Value("${matching.result-close-hour:11}") int resultCloseHour,
            Clock matchingClock
    ) {
        this(
                parseDays(festivalDaysCsv),
                LocalTime.of(registrationOpenHour, 0),
                LocalTime.of(registrationCloseHour, 0),
                LocalTime.of(resultOpenHour, 0),
                LocalTime.of(resultCloseHour, 0),
                matchingClock
        );
    }

    MatchingScheduleProperties(
            List<LocalDate> festivalDays,
            LocalTime registrationOpenTime,
            LocalTime registrationCloseTime,
            LocalTime resultOpenTime,
            LocalTime resultCloseTime,
            Clock clock
    ) {
        this.festivalDays = List.copyOf(festivalDays);
        this.registrationOpenTime = registrationOpenTime;
        this.registrationCloseTime = registrationCloseTime;
        this.resultOpenTime = resultOpenTime;
        this.resultCloseTime = resultCloseTime;
        this.clock = clock;
    }

    private static List<LocalDate> parseDays(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(LocalDate::parse)
                .sorted()
                .toList();
    }

    public boolean isRegistrationOpen() {
        return currentRegistrationDay().isPresent();
    }

    public boolean isResultOpen() {
        return currentResultDay().isPresent();
    }

    public Optional<LocalDate> currentRegistrationDay() {
        LocalDateTime now = nowDateTime();
        LocalDate today = now.toLocalDate();
        LocalTime time = now.toLocalTime();
        if (!festivalDays.contains(today)) {
            return Optional.empty();
        }
        if (!time.isBefore(registrationOpenTime) && time.isBefore(registrationCloseTime)) {
            return Optional.of(today);
        }
        return Optional.empty();
    }

    // 신청 마감 후 결과창 오픈 직전(21:00~22:00) 사이에 매칭 잡을 돌릴 대상 일자.
    public Optional<LocalDate> dayPendingMatching() {
        LocalDateTime now = nowDateTime();
        LocalDate today = now.toLocalDate();
        LocalTime time = now.toLocalTime();
        if (festivalDays.contains(today)
                && !time.isBefore(registrationCloseTime)
                && time.isBefore(resultOpenTime)) {
            return Optional.of(today);
        }
        return Optional.empty();
    }

    // 22:00(N일) ~ 11:00(N+1일) 사이에 호출되면 결과창에 표시할 신청 데이터는 N일 분이다.
    public Optional<LocalDate> currentResultDay() {
        LocalDateTime now = nowDateTime();
        LocalDate today = now.toLocalDate();
        LocalTime time = now.toLocalTime();

        if (festivalDays.contains(today) && !time.isBefore(resultOpenTime)) {
            return Optional.of(today);
        }
        LocalDate yesterday = today.minusDays(1);
        if (festivalDays.contains(yesterday) && time.isBefore(resultCloseTime)) {
            return Optional.of(yesterday);
        }
        return Optional.empty();
    }

    public List<LocalDate> festivalDays() {
        return festivalDays;
    }

    public LocalTime registrationOpenTime() {
        return registrationOpenTime;
    }

    public LocalTime registrationCloseTime() {
        return registrationCloseTime;
    }

    public LocalTime resultOpenTime() {
        return resultOpenTime;
    }

    public LocalTime resultCloseTime() {
        return resultCloseTime;
    }

    public String upcomingRegistrationDeadlineIso() {
        return atKst(targetDayForUpcoming(), registrationCloseTime);
    }

    public String upcomingResultOpenIso() {
        return atKst(targetDayForUpcoming(), resultOpenTime);
    }

    private LocalDate targetDayForUpcoming() {
        return currentRegistrationDay()
                .or(this::nextFestivalDay)
                .orElseGet(this::lastFestivalDay);
    }

    private Optional<LocalDate> nextFestivalDay() {
        LocalDate today = nowDateTime().toLocalDate();
        return festivalDays.stream().filter(d -> !d.isBefore(today)).findFirst();
    }

    private LocalDate lastFestivalDay() {
        if (festivalDays.isEmpty()) {
            throw new IllegalStateException("festival days is empty");
        }
        return festivalDays.get(festivalDays.size() - 1);
    }

    private String atKst(LocalDate day, LocalTime time) {
        return LocalDateTime.of(day, time).atZone(KOREA_ZONE).toOffsetDateTime().toString();
    }

    private LocalDateTime nowDateTime() {
        return ZonedDateTime.now(clock).toLocalDateTime();
    }
}
