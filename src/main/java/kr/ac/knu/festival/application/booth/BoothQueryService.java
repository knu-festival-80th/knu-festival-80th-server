package kr.ac.knu.festival.application.booth;

import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.domain.booth.entity.Menu;
import kr.ac.knu.festival.domain.booth.repository.BoothRepository;
import kr.ac.knu.festival.domain.booth.repository.MenuRepository;
import kr.ac.knu.festival.domain.waiting.entity.WaitingStatus;
import kr.ac.knu.festival.domain.waiting.repository.WaitingRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.presentation.booth.dto.response.BoothDetailResponse;
import kr.ac.knu.festival.presentation.booth.dto.response.BoothListResponse;
import kr.ac.knu.festival.presentation.booth.dto.response.BoothMapResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoothQueryService {

    private static final List<WaitingStatus> ACTIVE_STATUSES = List.of(WaitingStatus.WAITING, WaitingStatus.CALLED);
    private static final String SORT_LIKES = "likes";
    private static final String SORT_WAITING_ASC = "waiting-asc";

    private final BoothRepository boothRepository;
    private final MenuRepository menuRepository;
    private final WaitingRepository waitingRepository;

    public List<BoothListResponse> getBooths(String sort) {
        List<Booth> booths = boothRepository.findAllByOrderByLikeCountDescIdAsc();

        List<BoothListResponse> responses = booths.stream()
                .map(booth -> BoothListResponse.fromEntity(
                        booth,
                        waitingRepository.countByBoothIdAndStatusIn(booth.getId(), ACTIVE_STATUSES)))
                .toList();

        if (SORT_WAITING_ASC.equalsIgnoreCase(sort)) {
            return responses.stream()
                    .sorted(Comparator.comparingLong(BoothListResponse::currentWaitingTeams)
                            .thenComparing(Comparator.comparingInt(BoothListResponse::likeCount).reversed()))
                    .toList();
        }
        // SORT_LIKES (기본)
        return responses;
    }

    public BoothDetailResponse getBooth(Long boothId) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        List<Menu> menus = menuRepository.findAllByBoothIdOrderByIdAsc(boothId);
        long activeWaiting = waitingRepository.countByBoothIdAndStatusIn(boothId, ACTIVE_STATUSES);
        return BoothDetailResponse.of(booth, menus, activeWaiting);
    }

    public List<BoothMapResponse> getBoothsForMap() {
        return boothRepository.findAll().stream()
                .map(BoothMapResponse::fromEntity)
                .toList();
    }
}
