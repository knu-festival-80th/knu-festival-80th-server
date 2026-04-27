package kr.ac.knu.festival.application.waiting;

import kr.ac.knu.festival.domain.waiting.entity.Waiting;
import kr.ac.knu.festival.domain.waiting.repository.WaitingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * SMS 발송 결과를 DB에 반영하는 책임만 가진 빈.
 * WaitingCommandService 의 비동기 람다에서 self-invocation 으로 호출하면 Spring AOP 프록시가
 * 우회되어 @Transactional 이 무력화되므로, 별도 빈으로 분리해 트랜잭션 경계를 보장한다.
 */
@Component
@RequiredArgsConstructor
public class SmsStatusUpdater {

    private final WaitingRepository waitingRepository;

    @Transactional
    public void markSent(Long waitingId) {
        waitingRepository.findById(waitingId).ifPresent(Waiting::markSmsSent);
    }
}
