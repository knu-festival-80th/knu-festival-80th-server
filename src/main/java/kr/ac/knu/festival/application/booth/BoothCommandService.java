package kr.ac.knu.festival.application.booth;

import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.domain.booth.repository.BoothRepository;
import kr.ac.knu.festival.domain.waiting.entity.WaitingStatus;
import kr.ac.knu.festival.domain.waiting.repository.WaitingRepository;
import kr.ac.knu.festival.global.auth.AdminSessionRegistry;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.infra.redis.BoothRankingRedisRepository;
import kr.ac.knu.festival.infra.redis.BoothRankingRedisRepository.RedisChangeResult;
import kr.ac.knu.festival.infra.storage.ImageUrlResolver;
import kr.ac.knu.festival.presentation.booth.dto.request.BoothCreateRequest;
import kr.ac.knu.festival.presentation.booth.dto.request.BoothUpdateRequest;
import kr.ac.knu.festival.presentation.booth.dto.response.BoothResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BoothCommandService {

    private static final List<WaitingStatus> ACTIVE_STATUSES = List.of(WaitingStatus.WAITING, WaitingStatus.CALLED);

    private final BoothRepository boothRepository;
    private final WaitingRepository waitingRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageUrlResolver imageUrlResolver;
    private final BoothRankingRedisRepository boothRankingRedisRepository;
    private final BoothRankingStreamService boothRankingStreamService;
    private final AdminSessionRegistry adminSessionRegistry;

    public BoothResponse createBooth(BoothCreateRequest request) {
        Booth booth = Booth.createBooth(
                request.name(),
                request.xRatio(),
                request.yRatio(),
                request.menuBoardImageUrl(),
                passwordEncoder.encode(request.adminPassword()),
                request.department(),
                request.location()
        );
        Booth saved = boothRepository.save(booth);
        Long boothId = saved.getId();
        afterCommit(() -> {
            boothRankingRedisRepository.registerBooth(boothId);
            boothRankingStreamService.markDirty();
        });
        return BoothResponse.fromEntity(saved, imageUrlResolver);
    }

    public BoothResponse updateBooth(Long boothId, BoothUpdateRequest request) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        booth.updateBooth(
                request.name(),
                request.xRatio(),
                request.yRatio(),
                request.menuBoardImageUrl(),
                request.department(),
                request.location()
        );
        return BoothResponse.fromEntity(booth, imageUrlResolver);
    }

    public void deleteBooth(Long boothId) {
        Booth booth = boothRepository.findByIdForUpdate(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        long activeCount = waitingRepository.countByBoothIdAndStatusIn(boothId, ACTIVE_STATUSES);
        if (activeCount > 0) {
            throw new BusinessException(BusinessErrorCode.BOOTH_HAS_ACTIVE_WAITINGS);
        }
        boothRepository.delete(booth);
        afterCommit(() -> {
            boothRankingRedisRepository.evictBooth(boothId);
            boothRankingStreamService.markDirty();
        });
    }

    public void changeBoothPassword(Long boothId, String newRawPassword) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        booth.changeAdminPassword(passwordEncoder.encode(newRawPassword));
        // BR-AUTH: 비번 변경 후 해당 부스의 모든 활성 세션을 만료해 기존 토큰 재사용을 차단.
        afterCommit(() -> adminSessionRegistry.invalidateAllForBooth(boothId));
    }

    public BoothResponse likeBooth(Long boothId, String anonymousIdHash) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        RedisChangeResult result = boothRankingRedisRepository.addLike(boothId, anonymousIdHash);
        if (!result.available()) {
            log.warn("Redis unavailable for likeBooth. boothId={} — fallback에서는 중복 좋아요 방지 불가", boothId);
            boothRepository.incrementLike(boothId);
            // fallback 진입 시 sync 가 옛 Redis 값으로 DB 를 덮어쓰지 않도록 dirty 플래그를 제거한다.
            boothRankingRedisRepository.unmarkLikeDirty(boothId);
            boothRankingStreamService.markDirty();
            Booth updatedBooth = boothRepository.findById(boothId)
                    .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
            return BoothResponse.fromEntity(updatedBooth, imageUrlResolver);
        }
        if (result.changed()) {
            boothRankingStreamService.markDirty();
        }
        int likeCount = boothRankingRedisRepository.getLikeCount(boothId, booth.getLikeCount());
        return BoothResponse.fromEntity(booth, likeCount, imageUrlResolver);
    }

    public BoothResponse unlikeBooth(Long boothId, String anonymousIdHash) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        if (anonymousIdHash == null) {
            return BoothResponse.fromEntity(
                    booth,
                    boothRankingRedisRepository.getLikeCount(boothId, booth.getLikeCount()),
                    imageUrlResolver);
        }
        RedisChangeResult result = boothRankingRedisRepository.removeLike(boothId, anonymousIdHash);
        if (!result.available()) {
            log.warn("Redis unavailable for unlikeBooth. boothId={} — fallback으로 DB 직접 감소", boothId);
            boothRepository.decrementLike(boothId);
            boothRankingRedisRepository.unmarkLikeDirty(boothId);
            boothRankingStreamService.markDirty();
            Booth updatedBooth = boothRepository.findById(boothId)
                    .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
            return BoothResponse.fromEntity(updatedBooth, imageUrlResolver);
        }
        if (result.changed()) {
            boothRankingStreamService.markDirty();
        }
        int likeCount = boothRankingRedisRepository.getLikeCount(boothId, booth.getLikeCount());
        return BoothResponse.fromEntity(booth, likeCount, imageUrlResolver);
    }

    private void afterCommit(Runnable runnable) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runnable.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runnable.run();
            }
        });
    }
}
