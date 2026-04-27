package kr.ac.knu.festival.application.booth;

import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.domain.booth.entity.Menu;
import kr.ac.knu.festival.domain.booth.repository.BoothRepository;
import kr.ac.knu.festival.domain.booth.repository.MenuRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.presentation.booth.dto.request.MenuCreateRequest;
import kr.ac.knu.festival.presentation.booth.dto.request.MenuUpdateRequest;
import kr.ac.knu.festival.presentation.booth.dto.response.MenuResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuCommandService {

    private final BoothRepository boothRepository;
    private final MenuRepository menuRepository;

    public MenuResponse createMenu(Long boothId, MenuCreateRequest request) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        Menu menu = Menu.createMenu(
                booth,
                request.name(),
                request.price(),
                request.imageUrl(),
                request.description()
        );
        return MenuResponse.fromEntity(menuRepository.save(menu));
    }

    public MenuResponse updateMenu(Long boothId, Long menuId, MenuUpdateRequest request) {
        Menu menu = findMenuOwnedByBooth(boothId, menuId);
        menu.updateMenu(request.name(), request.price(), request.imageUrl(), request.description());
        return MenuResponse.fromEntity(menu);
    }

    public MenuResponse toggleSoldOut(Long boothId, Long menuId) {
        Menu menu = findMenuOwnedByBooth(boothId, menuId);
        menu.toggleSoldOut();
        return MenuResponse.fromEntity(menu);
    }

    public void deleteMenu(Long boothId, Long menuId) {
        Menu menu = findMenuOwnedByBooth(boothId, menuId);
        menuRepository.delete(menu);
    }

    private Menu findMenuOwnedByBooth(Long boothId, Long menuId) {
        return menuRepository.findByIdAndBoothId(menuId, boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MENU_NOT_FOUND));
    }
}
