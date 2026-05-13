package kr.ac.knu.festival.application.matching;

import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingJobResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingAutoScheduler {

    private final MatchingScheduleProperties matchingScheduleProperties;
    private final MatchingParticipantRepository matchingParticipantRepository;
    private final MatchingCommandService matchingCommandService;

    @Scheduled(fixedDelayString = "${matching.auto-job-delay-ms:60000}")
    public void runDuringMatchingWindow() {
        // 21:00~22:00 사이에 한정. 서버 재시작 대비해 한 번이 아니라 주기적으로 확인한다.
        Optional<LocalDate> dayOpt = matchingScheduleProperties.dayPendingMatching();
        if (dayOpt.isEmpty()) {
            return;
        }
        LocalDate day = dayOpt.get();
        long pending = matchingParticipantRepository.countByFestivalDayAndStatus(day, MatchingParticipantStatus.PENDING);
        if (pending == 0) {
            return;
        }

        MatchingJobResponse result = matchingCommandService.runMatchingJobFor(day);
        log.info(
                "Auto matching job finished: day={}, pendingBefore={}, matchedPairCount={}, unmatchedCount={}",
                day, pending, result.matchedPairCount(), result.unmatchedCount()
        );
    }
}
