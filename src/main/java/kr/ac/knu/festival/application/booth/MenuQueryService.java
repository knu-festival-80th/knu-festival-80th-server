package kr.ac.knu.festival.application.booth;

import kr.ac.knu.festival.domain.booth.repository.BoothRepository;
import kr.ac.knu.festival.domain.booth.repository.MenuRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.presentation.booth.dto.response.MenuResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuQueryService {

    private final BoothRepository boothRepository;
    private final MenuRepository menuRepository;

    public List<MenuResponse> getMenus(Long boothId) {
        if (!boothRepository.existsById(boothId)) {
            throw new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND);
        }
        return menuRepository.findAllByBoothIdOrderByIdAsc(boothId).stream()
                .map(MenuResponse::fromEntity)
                .toList();
    }
}
