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
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingAuthRequest;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingCreateRequest;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingStatusUpdateRequest;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingJobResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingRegisterResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchingCommandService {

    private static final String DEFAULT_NATIONALITY = "KR";

    private final MatchingParticipantRepository matchingParticipantRepository;
    private final MatchingServiceStateRepository matchingServiceStateRepository;
    private final PasswordEncoder passwordEncoder;

    public MatchingRegisterResponse register(MatchingCreateRequest request) {
        MatchingServiceState state = getOrCreateState();
        if (state.getStatus() != MatchingOperationStatus.OPEN) {
            throw new BusinessException(BusinessErrorCode.INVALID_INPUT_VALUE);
        }

        // Instagram ID는 @ 입력 여부와 대소문자 차이로 중복 신청이 뚫리지 않도록 저장 전에 정규화한다.
        String instagramId = normalizeInstagramId(request.instagramId());
        if (matchingParticipantRepository.existsById(instagramId)) {
            throw new BusinessException(BusinessErrorCode.INVALID_INPUT_VALUE);
        }

        MatchingParticipant participant = MatchingParticipant.create(
                instagramId,
                request.gender(),
                // 결과 조회/취소에 쓰는 비밀번호는 원문 저장 금지. BCrypt 해시만 DB에 남긴다.
                passwordEncoder.encode(request.password()),
                normalizeNationality(request.nationality())
        );
        return MatchingRegisterResponse.fromEntity(matchingParticipantRepository.save(participant));
    }

    public void cancel(MatchingAuthRequest request) {
        MatchingParticipant participant = authenticate(request);
        if (participant.getStatus() == MatchingParticipantStatus.MATCHED) {
            throw new BusinessException(BusinessErrorCode.INVALID_INPUT_VALUE);
        }
        participant.cancel();
    }

    public MatchingJobResponse runMatchingJob() {
        // Time Drop 실행 중 같은 PENDING 참가자가 중복 매칭되지 않도록 성별별 행 락을 잡는다.
        List<MatchingParticipant> males = matchingParticipantRepository.findAllByStatusAndGenderForUpdate(
                MatchingParticipantStatus.PENDING, MatchingGender.MALE);
        List<MatchingParticipant> females = matchingParticipantRepository.findAllByStatusAndGenderForUpdate(
                MatchingParticipantStatus.PENDING, MatchingGender.FEMALE);

        // 명세상 성별 한쪽이 70% 이상이면 매칭을 멈추고 상태 API로 안내 메시지를 노출한다.
        if (pauseWhenGenderImbalanced(males.size(), females.size())) {
            return new MatchingJobResponse(0, males.size() + females.size());
        }

        Collections.shuffle(males);
        Collections.shuffle(females);

        int pairCount = Math.min(males.size(), females.size());
        for (int i = 0; i < pairCount; i++) {
            MatchingParticipant male = males.get(i);
            MatchingParticipant female = females.get(i);
            male.matchWith(female.getInstagramId());
            female.matchWith(male.getInstagramId());
        }

        // 남녀 수가 맞지 않아 남은 참가자는 공개 목록 API에서 조회할 수 있도록 UNMATCHED로 전환한다.
        for (int i = pairCount; i < males.size(); i++) {
            males.get(i).markUnmatched();
        }
        for (int i = pairCount; i < females.size(); i++) {
            females.get(i).markUnmatched();
        }

        return new MatchingJobResponse(pairCount, males.size() + females.size() - pairCount * 2);
    }

    public MatchingStatusResponse updateStatus(MatchingStatusUpdateRequest request) {
        MatchingServiceState state = getOrCreateState();
        state.changeStatus(
                request.status(),
                defaultMessageKo(request.status(), request.messageKo()),
                defaultMessageEn(request.status(), request.messageEn())
        );
        return MatchingStatusResponse.fromEntity(state);
    }

    private MatchingParticipant authenticate(MatchingAuthRequest request) {
        MatchingParticipant participant = matchingParticipantRepository.findById(normalizeInstagramId(request.instagramId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND));
        if (!passwordEncoder.matches(request.password(), participant.getPassword())) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED_USER);
        }
        return participant;
    }

    private MatchingServiceState getOrCreateState() {
        // 운영 상태는 전역 값 하나만 필요하므로 state_id=1 단일 행으로 관리한다.
        return matchingServiceStateRepository.findById(MatchingServiceState.SINGLETON_ID)
                .orElseGet(() -> matchingServiceStateRepository.save(MatchingServiceState.defaultOpen()));
    }

    private boolean pauseWhenGenderImbalanced(int maleCount, int femaleCount) {
        int total = maleCount + femaleCount;
        if (total == 0) {
            return false;
        }
        if ((double) Math.max(maleCount, femaleCount) / total >= 0.7) {
            MatchingServiceState state = getOrCreateState();
            state.changeStatus(
                    MatchingOperationStatus.PAUSED,
                    "성별 비율 불균형으로 매칭이 일시중단되었습니다.",
                    "Matching is paused because the gender ratio is imbalanced."
            );
            return true;
        }
        return false;
    }

    private String normalizeInstagramId(String instagramId) {
        return instagramId.trim().replaceFirst("^@", "").toLowerCase();
    }

    private String normalizeNationality(String nationality) {
        if (nationality == null || nationality.isBlank()) {
            return DEFAULT_NATIONALITY;
        }
        // 국적 코드는 화면 언어 분기 등에 재사용하기 쉽도록 대문자 코드로 맞춘다.
        return nationality.trim().toUpperCase();
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
}
