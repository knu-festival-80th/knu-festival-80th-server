package kr.ac.knu.festival.application.booth;

import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.domain.booth.repository.BoothRepository;
import kr.ac.knu.festival.domain.waiting.entity.WaitingStatus;
import kr.ac.knu.festival.domain.waiting.repository.WaitingRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.presentation.booth.dto.request.BoothCreateRequest;
import kr.ac.knu.festival.presentation.booth.dto.request.BoothUpdateRequest;
import kr.ac.knu.festival.presentation.booth.dto.response.BoothResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BoothCommandService {

    private static final List<WaitingStatus> ACTIVE_STATUSES = List.of(WaitingStatus.WAITING, WaitingStatus.CALLED);

    private final BoothRepository boothRepository;
    private final WaitingRepository waitingRepository;
    private final PasswordEncoder passwordEncoder;

    public BoothResponse createBooth(BoothCreateRequest request) {
        Booth booth = Booth.createBooth(
                request.name(),
                request.description(),
                request.xRatio(),
                request.yRatio(),
                request.imageUrl(),
                request.menuBoardImageUrl(),
                passwordEncoder.encode(request.adminPassword())
        );
        return BoothResponse.fromEntity(boothRepository.save(booth));
    }

    public BoothResponse updateBooth(Long boothId, BoothUpdateRequest request) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        booth.updateBooth(
                request.name(),
                request.description(),
                request.xRatio(),
                request.yRatio(),
                request.imageUrl(),
                request.menuBoardImageUrl()
        );
        return BoothResponse.fromEntity(booth);
    }

    public void deleteBooth(Long boothId) {
        Booth booth = boothRepository.findByIdForUpdate(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        long activeCount = waitingRepository.countByBoothIdAndStatusIn(boothId, ACTIVE_STATUSES);
        if (activeCount > 0) {
            throw new BusinessException(BusinessErrorCode.BOOTH_HAS_ACTIVE_WAITINGS);
        }
        boothRepository.delete(booth);
    }

    public void changeBoothPassword(Long boothId, String newRawPassword) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        booth.changeAdminPassword(passwordEncoder.encode(newRawPassword));
    }

    public BoothResponse likeBooth(Long boothId) {
        int updated = boothRepository.incrementLike(boothId);
        if (updated == 0) {
            throw new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND);
        }
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        return BoothResponse.fromEntity(booth);
    }

    public BoothResponse unlikeBooth(Long boothId) {
        boothRepository.decrementLike(boothId);
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        return BoothResponse.fromEntity(booth);
    }
}
