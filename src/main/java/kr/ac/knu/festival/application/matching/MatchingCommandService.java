package kr.ac.knu.festival.application.matching;

import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingOperationStatus;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
import kr.ac.knu.festival.domain.matching.entity.MatchingServiceState;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.domain.matching.repository.MatchingServiceStateRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingCreateRequest;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingStatusUpdateRequest;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingJobResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingRegisterResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchingCommandService {

    private final MatchingParticipantRepository matchingParticipantRepository;
    private final MatchingServiceStateRepository matchingServiceStateRepository;
    private final PasswordEncoder passwordEncoder;
    private final MatchingScheduleProperties matchingScheduleProperties;
    private final MatchingRealtimeCache matchingRealtimeCache;

    public MatchingRegisterResponse register(MatchingCreateRequest request) {
        MatchingServiceState state = getOrCreateState();
        if (state.getStatus() != MatchingOperationStatus.OPEN || !matchingScheduleProperties.isRegistrationOpen()) {
            throw new BusinessException(BusinessErrorCode.MATCHING_REGISTRATION_CLOSED);
        }

        String instagramId = MatchingParticipant.normalizeInstagramId(request.instagramId());
        if (matchingParticipantRepository.existsById(instagramId)) {
            throw new BusinessException(BusinessErrorCode.MATCHING_DUPLICATE_REGISTRATION);
        }

        MatchingParticipant participant = MatchingParticipant.create(
                instagramId,
                request.gender(),
                passwordEncoder.encode(request.password())
        );
        MatchingParticipant saved = matchingParticipantRepository.save(participant);
        matchingRealtimeCache.cacheParticipantResult(saved);
        refreshRealtimeStatus(state);
        return MatchingRegisterResponse.fromEntity(
                saved,
                matchingScheduleProperties.registrationDeadline().toString(),
                matchingScheduleProperties.resultOpenAt().toString()
        );
    }

    public MatchingJobResponse runMatchingJob() {
        List<MatchingParticipant> males = matchingParticipantRepository.findAllByStatusAndGenderForUpdate(
                MatchingParticipantStatus.PENDING, MatchingGender.MALE);
        List<MatchingParticipant> females = matchingParticipantRepository.findAllByStatusAndGenderForUpdate(
                MatchingParticipantStatus.PENDING, MatchingGender.FEMALE);

        int matchableCount = Math.min(males.size(), females.size());
        List<MatchingParticipant> eligibleMales = new ArrayList<>(males.subList(0, matchableCount));
        List<MatchingParticipant> eligibleFemales = new ArrayList<>(females.subList(0, matchableCount));

        List<MatchingParticipant> unmatchedParticipants = new ArrayList<>();
        unmatchedParticipants.addAll(males.subList(matchableCount, males.size()));
        unmatchedParticipants.addAll(females.subList(matchableCount, females.size()));

        List<MatchingParticipant> shuffledFemales = new ArrayList<>(eligibleFemales);
        Collections.shuffle(shuffledFemales);
        for (int i = 0; i < eligibleMales.size(); i++) {
            MatchingParticipant male = eligibleMales.get(i);
            male.matchWith(shuffledFemales.get(i).getInstagramId());
            matchingRealtimeCache.cacheParticipantResult(male);
        }

        List<MatchingParticipant> shuffledMales = new ArrayList<>(eligibleMales);
        Collections.shuffle(shuffledMales);
        for (int i = 0; i < eligibleFemales.size(); i++) {
            MatchingParticipant female = eligibleFemales.get(i);
            female.matchWith(shuffledMales.get(i).getInstagramId());
            matchingRealtimeCache.cacheParticipantResult(female);
        }

        for (MatchingParticipant participant : unmatchedParticipants) {
            participant.markUnmatched();
            matchingRealtimeCache.cacheParticipantResult(participant);
        }

        refreshRealtimeStatus(getOrCreateState());
        return new MatchingJobResponse(matchableCount, unmatchedParticipants.size());
    }

    public MatchingStatusResponse updateStatus(MatchingStatusUpdateRequest request) {
        MatchingServiceState state = getOrCreateState();
        state.changeStatus(
                request.status(),
                defaultMessageKo(request.status(), request.messageKo()),
                defaultMessageEn(request.status(), request.messageEn())
        );
        return refreshRealtimeStatus(state);
    }

    private MatchingServiceState getOrCreateState() {
        // 운영 상태는 전역 값 하나만 필요하므로 state_id=1 단일 행으로 관리한다.
        return matchingServiceStateRepository.findById(MatchingServiceState.SINGLETON_ID)
                .orElseGet(() -> matchingServiceStateRepository.save(MatchingServiceState.defaultOpen()));
    }

    private String defaultMessageKo(MatchingOperationStatus status, String message) {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return status == MatchingOperationStatus.OPEN ? "매칭 신청이 가능합니다." : "매칭 신청이 일시중단되었습니다.";
    }

    private String defaultMessageEn(MatchingOperationStatus status, String message) {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return status == MatchingOperationStatus.OPEN ? "Matching is open." : "Matching is paused.";
    }

    private MatchingStatusResponse refreshRealtimeStatus(MatchingServiceState state) {
        // 상태 API는 프론트가 자주 폴링할 수 있으므로, DB 상태를 계산한 직후 Redis 캐시도 같이 갱신한다.
        long pendingCount = matchingParticipantRepository.countByStatus(MatchingParticipantStatus.PENDING);
        long matchedCount = matchingParticipantRepository.countByStatus(MatchingParticipantStatus.MATCHED);
        long unmatchedCount = matchingParticipantRepository.countByStatus(MatchingParticipantStatus.UNMATCHED);
        MatchingStatusResponse response = MatchingStatusResponse.of(
                state,
                state.getStatus() == MatchingOperationStatus.OPEN && matchingScheduleProperties.isRegistrationOpen(),
                matchingScheduleProperties.isResultOpen(),
                matchingScheduleProperties.registrationDeadline().toString(),
                matchingScheduleProperties.resultOpenAt().toString(),
                pendingCount,
                matchedCount,
                unmatchedCount
        );
        matchingRealtimeCache.cacheStatus(state, matchingScheduleProperties, pendingCount, matchedCount, unmatchedCount);
        return response;
    }
}
