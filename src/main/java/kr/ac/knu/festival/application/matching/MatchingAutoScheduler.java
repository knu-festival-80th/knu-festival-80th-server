package kr.ac.knu.festival.application.matching;

import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingJobResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingAutoScheduler {

    private final MatchingScheduleProperties matchingScheduleProperties;
    private final MatchingParticipantRepository matchingParticipantRepository;
    private final MatchingCommandService matchingCommandService;

    @Scheduled(fixedDelayString = "${matching.auto-job-delay-ms:60000}")
    public void runAfterRegistrationDeadline() {
        // 현재 기본 정책은 2026-05-21 21:00(KST) 신청 마감, 22:00(KST) 결과 공개다.
        // 시간은 추후 matching.registration-deadline / matching.result-open-at 환경값으로 바꾸면 된다.
        // 21~22시 사이에 서버가 재시작될 수 있으므로, 한 번만 실행하지 않고 주기적으로 PENDING 여부를 확인한다.
        if (!matchingScheduleProperties.isRegistrationClosed()) {
            return;
        }

        long pendingCount = matchingParticipantRepository.countByStatus(MatchingParticipantStatus.PENDING);
        if (pendingCount == 0) {
            return;
        }

        // runMatchingJob()은 PENDING만 대상으로 삼기 때문에 반복 호출되어도 이미 매칭된 참가자는 다시 섞이지 않는다.
        MatchingJobResponse result = matchingCommandService.runMatchingJob();
        log.info(
                "Auto matching job finished: pendingBefore={}, matchedPairCount={}, unmatchedCount={}",
                pendingCount,
                result.matchedPairCount(),
                result.unmatchedCount()
        );
    }
}
