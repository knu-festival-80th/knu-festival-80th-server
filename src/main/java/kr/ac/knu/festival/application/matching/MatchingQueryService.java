package kr.ac.knu.festival.application.matching;

import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
import kr.ac.knu.festival.domain.matching.entity.MatchingServiceState;
import kr.ac.knu.festival.domain.matching.repository.MatchingParticipantRepository;
import kr.ac.knu.festival.domain.matching.repository.MatchingServiceStateRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.presentation.matching.dto.request.MatchingAuthRequest;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingResultResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.MatchingStatusResponse;
import kr.ac.knu.festival.presentation.matching.dto.response.UnmatchedParticipantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingQueryService {

    private final MatchingParticipantRepository matchingParticipantRepository;
    private final MatchingServiceStateRepository matchingServiceStateRepository;
    private final PasswordEncoder passwordEncoder;

    public MatchingResultResponse getResult(MatchingAuthRequest request) {
        MatchingParticipant participant = matchingParticipantRepository.findById(normalizeInstagramId(request.instagramId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND));
        if (!passwordEncoder.matches(request.password(), participant.getPassword())) {
            throw new BusinessException(BusinessErrorCode.UNAUTHORIZED_USER);
        }
        return MatchingResultResponse.fromEntity(participant);
    }

    public MatchingStatusResponse getStatus() {
        MatchingServiceState state = matchingServiceStateRepository.findById(MatchingServiceState.SINGLETON_ID)
                .orElse(MatchingServiceState.defaultOpen());
        return MatchingStatusResponse.fromEntity(state);
    }

    public List<UnmatchedParticipantResponse> getUnmatchedParticipants() {
        return matchingParticipantRepository.findAllByStatus(MatchingParticipantStatus.UNMATCHED).stream()
                .map(UnmatchedParticipantResponse::fromEntity)
                .toList();
    }

    private String normalizeInstagramId(String instagramId) {
        return instagramId.trim().replaceFirst("^@", "").toLowerCase();
    }
}
