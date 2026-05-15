package kr.ac.knu.festival.application.waiting;

import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.domain.booth.repository.BoothRepository;
import kr.ac.knu.festival.domain.waiting.entity.Waiting;
import kr.ac.knu.festival.domain.waiting.entity.WaitingStatus;
import kr.ac.knu.festival.domain.waiting.repository.WaitingRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.application.booth.BoothRankingStreamService;
import kr.ac.knu.festival.infra.redis.BoothRankingRedisRepository;
import kr.ac.knu.festival.infra.security.PhoneLookupHasher;
import kr.ac.knu.festival.infra.security.PhoneNumberEncryptor;
import kr.ac.knu.festival.infra.sms.SmsSender;
import kr.ac.knu.festival.presentation.waiting.dto.request.WaitingCreateRequest;
import kr.ac.knu.festival.presentation.waiting.dto.request.WaitingInsertRequest;
import kr.ac.knu.festival.presentation.waiting.dto.request.WaitingReorderRequest;
import kr.ac.knu.festival.presentation.waiting.dto.response.WaitingRegisterResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@Transactional
public class WaitingCommandService {

    private static final List<WaitingStatus> ACTIVE_STATUSES = List.of(WaitingStatus.WAITING, WaitingStatus.CALLED);
    private static final int MINUTES_PER_TEAM = 5;
    private static final int MAX_ACTIVE_WAITINGS = 3;

    private static final String PHONE_LOCK_KEY_PREFIX = "waiting:lock:";
    private static final Duration PHONE_LOCK_TTL = Duration.ofSeconds(5);

    private final BoothRepository boothRepository;
    private final WaitingRepository waitingRepository;
    private final PhoneNumberEncryptor phoneNumberEncryptor;
    private final PhoneLookupHasher phoneLookupHasher;
    private final SmsSender smsSender;
    private final SmsStatusUpdater smsStatusUpdater;
    private final ThreadPoolTaskExecutor smsExecutor;
    private final BoothRankingRedisRepository boothRankingRedisRepository;
    private final BoothRankingStreamService boothRankingStreamService;
    private final WaitingRateLimiter waitingRateLimiter;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public WaitingCommandService(
            BoothRepository boothRepository,
            WaitingRepository waitingRepository,
            PhoneNumberEncryptor phoneNumberEncryptor,
            PhoneLookupHasher phoneLookupHasher,
            SmsSender smsSender,
            SmsStatusUpdater smsStatusUpdater,
            @Qualifier("smsExecutor") ThreadPoolTaskExecutor smsExecutor,
            BoothRankingRedisRepository boothRankingRedisRepository,
            BoothRankingStreamService boothRankingStreamService,
            WaitingRateLimiter waitingRateLimiter,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider
    ) {
        this.boothRepository = boothRepository;
        this.waitingRepository = waitingRepository;
        this.phoneNumberEncryptor = phoneNumberEncryptor;
        this.phoneLookupHasher = phoneLookupHasher;
        this.smsSender = smsSender;
        this.smsStatusUpdater = smsStatusUpdater;
        this.smsExecutor = smsExecutor;
        this.boothRankingRedisRepository = boothRankingRedisRepository;
        this.boothRankingStreamService = boothRankingStreamService;
        this.waitingRateLimiter = waitingRateLimiter;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    public WaitingRegisterResponse registerWaiting(Long boothId, WaitingCreateRequest request, String clientIp) {
        waitingRateLimiter.recordRegistration(clientIp);

        String normalizedPhone = normalizePhoneNumber(request.phoneNumber());
        String lookupHash = phoneLookupHasher.hash(normalizedPhone);
        String encryptedPhone = phoneNumberEncryptor.encrypt(normalizedPhone);

        // BR-WAIT-12: 동일 사용자가 서로 다른 부스 A·B·C 에 동시에 등록해 3건 제한을 우회하는 race 차단.
        boolean lockAcquired = acquirePhoneLock(lookupHash);
        try {
            Booth booth = boothRepository.findByIdForUpdate(boothId)
                    .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
            if (!booth.isWaitingOpen()) {
                throw new BusinessException(BusinessErrorCode.WAITING_REGISTRATION_CLOSED);
            }

            waitingRepository.findFirstActiveByBoothAndPhoneLookupHash(boothId, lookupHash, ACTIVE_STATUSES)
                    .ifPresent(existing -> {
                        throw new BusinessException(BusinessErrorCode.DUPLICATE_WAITING);
                    });

            long totalActive = waitingRepository.countByPhoneLookupHashAndStatusIn(lookupHash, ACTIVE_STATUSES);
            if (totalActive >= MAX_ACTIVE_WAITINGS) {
                throw new BusinessException(BusinessErrorCode.WAITING_LIMIT_EXCEEDED);
            }

            if (totalActive > 0) {
                List<String> existingNames = waitingRepository.findDistinctNamesByPhoneLookupHashAndStatusIn(lookupHash, ACTIVE_STATUSES);
                boolean nameMatches = existingNames.stream().anyMatch(n -> n.equals(request.name()));
                if (!nameMatches) {
                    throw new BusinessException(BusinessErrorCode.WAITING_NAME_MISMATCH);
                }
            }

            int nextNumber = waitingRepository.findMaxWaitingNumberByBoothId(boothId) + 1;
            int nextSortOrder = waitingRepository.findMaxSortOrderByBoothId(boothId) + 1;

            Waiting saved = waitingRepository.save(Waiting.createWaiting(
                    booth, nextNumber, nextSortOrder,
                    request.name(), request.partySize(), encryptedPhone, lookupHash
            ));

            long currentWaitingTeams = waitingRepository.countByBoothIdAndStatusIn(boothId, ACTIVE_STATUSES);
            int estimatedWaitMinutes = Math.max(0, (int) (currentWaitingTeams - 1)) * MINUTES_PER_TEAM;
            incrementWaitingCountAfterCommit(boothId);

            return WaitingRegisterResponse.of(saved, booth, currentWaitingTeams, estimatedWaitMinutes);
        } finally {
            if (lockAcquired) {
                releasePhoneLockAfterCommit(lookupHash);
            }
        }
    }

    public void callWaiting(Long waitingId) {
        Waiting waiting = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.WAITING_NOT_FOUND));
        waiting.markCalled();
        String message = "[%s] %d번 손님 입장 차례! 10분 내 도착해 주세요."
                .formatted(waiting.getBooth().getName(), waiting.getWaitingNumber());
        sendSmsAfterCommit(waiting.getId(), waiting.getPhoneNumber(), message);
    }

    public void enterWaiting(Long waitingId) {
        Waiting waiting = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.WAITING_NOT_FOUND));
        WaitingStatus previousStatus = waiting.getStatus();
        waiting.markEntered();
        decrementWaitingCountIfBecameInactiveAfterCommit(waiting.getBooth().getId(), previousStatus, waiting.getStatus());

        String lookupHash = waiting.getPhoneLookupHash();
        // 일괄취소 경로도 phone-hash 락으로 보호해 등록 race 와 충돌이 없도록 한다.
        boolean lockAcquired = acquirePhoneLock(lookupHash);
        try {
            List<Waiting> otherActiveWaitings = waitingRepository.findActiveByPhoneLookupHashExcludingBooth(
                    lookupHash, ACTIVE_STATUSES, waiting.getBooth().getId());

            if (!otherActiveWaitings.isEmpty()) {
                List<String> cancelledBoothNames = new java.util.ArrayList<>();
                for (Waiting other : otherActiveWaitings) {
                    WaitingStatus otherPrevious = other.getStatus();
                    other.markCancelled();
                    decrementWaitingCountIfBecameInactiveAfterCommit(other.getBooth().getId(), otherPrevious, other.getStatus());
                    cancelledBoothNames.add(other.getBooth().getName());
                }
                String boothList = String.join(", ", cancelledBoothNames);
                String cancelMsg = "[%s] 대기가 자동 취소되었습니다.".formatted(boothList);
                sendSmsAfterCommit(otherActiveWaitings.get(0).getId(), waiting.getPhoneNumber(), cancelMsg);
            }
        } finally {
            if (lockAcquired) {
                releasePhoneLockAfterCommit(lookupHash);
            }
        }
    }

    public void cancelWaiting(Long waitingId) {
        Waiting waiting = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.WAITING_NOT_FOUND));
        WaitingStatus previousStatus = waiting.getStatus();
        waiting.markCancelled();
        decrementWaitingCountIfBecameInactiveAfterCommit(waiting.getBooth().getId(), previousStatus, waiting.getStatus());
        // BR-WAIT-16: 관리자 취소 시 SMS 안내.
        if (previousStatus == WaitingStatus.CALLED) {
            String message = "[%s] 대기가 취소되었습니다.".formatted(waiting.getBooth().getName());
            sendSmsAfterCommit(waiting.getId(), waiting.getPhoneNumber(), message);
        }
    }

    public void cancelWaitingByOwner(Long waitingId, String phoneLast4) {
        Waiting waiting = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.WAITING_NOT_FOUND));
        if (!phoneNumberEncryptor.matchesLast4(waiting.getPhoneNumber(), phoneLast4)) {
            throw new BusinessException(BusinessErrorCode.PHONE_VERIFICATION_FAILED);
        }
        WaitingStatus previousStatus = waiting.getStatus();
        waiting.markCancelled();
        decrementWaitingCountIfBecameInactiveAfterCommit(waiting.getBooth().getId(), previousStatus, waiting.getStatus());
    }

    public void skipWaiting(Long waitingId) {
        Waiting waiting = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.WAITING_NOT_FOUND));
        WaitingStatus previousStatus = waiting.getStatus();
        waiting.markSkipped();
        decrementWaitingCountIfBecameInactiveAfterCommit(waiting.getBooth().getId(), previousStatus, waiting.getStatus());
    }

    public WaitingRegisterResponse insertWaiting(Long boothId, WaitingInsertRequest request) {
        Booth booth = boothRepository.findByIdForUpdate(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));

        String normalizedPhone = normalizePhoneNumber(request.phoneNumber());
        String lookupHash = phoneLookupHasher.hash(normalizedPhone);
        String encryptedPhone = phoneNumberEncryptor.encrypt(normalizedPhone);

        waitingRepository.findFirstActiveByBoothAndPhoneLookupHash(boothId, lookupHash, ACTIVE_STATUSES)
                .ifPresent(existing -> {
                    throw new BusinessException(BusinessErrorCode.DUPLICATE_WAITING);
                });

        int insertAt = request.insertAfterSortOrder();
        waitingRepository.shiftSortOrdersUp(boothId, insertAt);

        int nextNumber = waitingRepository.findMaxWaitingNumberByBoothId(boothId) + 1;
        Waiting saved = waitingRepository.save(Waiting.createWaiting(
                booth, nextNumber, insertAt + 1,
                request.name(), request.partySize(), encryptedPhone, lookupHash
        ));

        long currentWaitingTeams = waitingRepository.countByBoothIdAndStatusIn(boothId, ACTIVE_STATUSES);
        int estimatedWaitMinutes = Math.max(0, (int) (currentWaitingTeams - 1)) * MINUTES_PER_TEAM;
        incrementWaitingCountAfterCommit(boothId);
        return WaitingRegisterResponse.of(saved, booth, currentWaitingTeams, estimatedWaitMinutes);
    }

    public void reorderWaiting(Long waitingId, WaitingReorderRequest request) {
        Waiting target = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.WAITING_NOT_FOUND));
        Long boothId = target.getBooth().getId();
        boothRepository.findByIdForUpdate(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));

        int oldOrder = target.getSortOrder();
        int newOrder = request.newSortOrder();
        if (oldOrder == newOrder) {
            return;
        }

        List<Waiting> all = waitingRepository.findAllByBoothIdOrderBySortOrderAsc(boothId);
        for (Waiting w : all) {
            if (w.getId().equals(target.getId())) {
                continue;
            }
            int order = w.getSortOrder();
            if (oldOrder < newOrder && order > oldOrder && order <= newOrder) {
                w.updateSortOrder(order - 1);
            } else if (newOrder < oldOrder && order >= newOrder && order < oldOrder) {
                w.updateSortOrder(order + 1);
            }
        }
        target.updateSortOrder(newOrder);
    }

    public void toggleBoothWaiting(Long boothId, boolean open) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        booth.toggleWaitingOpen(open);
    }

    public void resendSms(Long waitingId) {
        Waiting waiting = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.WAITING_NOT_FOUND));
        // BR-WAIT-09: 호출/스킵/입장확정 취소 SMS 모두 재발송 대상이라 CALLED·SKIPPED·ENTERED 모두 허용.
        WaitingStatus status = waiting.getStatus();
        if (status != WaitingStatus.CALLED && status != WaitingStatus.SKIPPED && status != WaitingStatus.ENTERED) {
            throw new BusinessException(BusinessErrorCode.INVALID_WAITING_STATUS_TRANSITION);
        }
        waiting.markSmsFailed();
        String message = resendMessageFor(waiting);
        sendSmsAfterCommit(waiting.getId(), waiting.getPhoneNumber(), message);
    }

    private String resendMessageFor(Waiting waiting) {
        String boothName = waiting.getBooth().getName();
        return switch (waiting.getStatus()) {
            case CALLED -> "[%s] %d번 손님 입장 차례! 10분 내 도착해 주세요."
                    .formatted(boothName, waiting.getWaitingNumber());
            case SKIPPED -> "[%s] %d번 대기가 시간 초과로 취소되었습니다."
                    .formatted(boothName, waiting.getWaitingNumber());
            case ENTERED -> "[%s] 입장 확정으로 다른 부스 대기가 자동 취소되었습니다.".formatted(boothName);
            default -> "[%s] 대기 안내".formatted(boothName);
        };
    }

    /**
     * SMS 발송을 트랜잭션 커밋 이후로 미룬다. AFTER_COMMIT 패턴은:
     *  - 비동기 worker 가 아직 commit 되지 않은 행을 조회해 NotFound 처리되는 문제 (SmsStatusUpdater.markSent),
     *  - 커밋 전에 외부 호출이 일어나 롤백 시 SMS 가 잘못 전송되는 문제
     * 를 동시에 해결한다.
     */
    public void sendSmsAfterCommit(Long waitingId, String encryptedPhone, String message) {
        afterCommit(() -> dispatchSmsAsync(waitingId, encryptedPhone, message));
    }

    /**
     * @deprecated AFTER_COMMIT 보장이 필요한 호출 지점은 sendSmsAfterCommit 을 사용. 직접 호출은 권장하지 않는다.
     */
    @Deprecated
    public void sendSmsAsync(Long waitingId, String encryptedPhone, String message) {
        dispatchSmsAsync(waitingId, encryptedPhone, message);
    }

    private void dispatchSmsAsync(Long waitingId, String encryptedPhone, String message) {
        String plainPhone = phoneNumberEncryptor.decrypt(encryptedPhone);
        if (plainPhone == null) {
            log.error("Phone decrypt failed for waiting {}", waitingId);
            return;
        }
        smsExecutor.execute(() -> {
            try {
                boolean success = smsSender.send(plainPhone, message);
                if (success) {
                    smsStatusUpdater.markSent(waitingId);
                }
            } catch (Exception ex) {
                log.warn("SMS dispatch failed for waiting {}: {}", waitingId, ex.getMessage());
            }
        });
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_PHONE_NUMBER);
        }
        return phoneNumber.replaceAll("\\D", "");
    }

    private void incrementWaitingCountAfterCommit(Long boothId) {
        afterCommit(() -> {
            boothRankingRedisRepository.incrementWaitingCount(boothId);
            boothRankingStreamService.markDirty();
        });
    }

    private void decrementWaitingCountIfBecameInactiveAfterCommit(
            Long boothId,
            WaitingStatus previousStatus,
            WaitingStatus currentStatus
    ) {
        if (!previousStatus.isActive() || currentStatus.isActive()) {
            return;
        }
        afterCommit(() -> {
            boothRankingRedisRepository.decrementWaitingCount(boothId);
            boothRankingStreamService.markDirty();
        });
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

    /**
     * phone hash 기반 분산 락. Redis SETNX + TTL. Redis 가 없으면 락 없이 통과한다
     * (in-memory 환경에서는 단일 노드라 부스 락만으로도 충돌이 거의 없음).
     */
    private boolean acquirePhoneLock(String phoneLookupHash) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return false;
        }
        try {
            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent(phoneLockKey(phoneLookupHash), "1", PHONE_LOCK_TTL);
            if (Boolean.TRUE.equals(ok)) {
                return true;
            }
            throw new BusinessException(BusinessErrorCode.WAITING_CONCURRENT_REGISTRATION);
        } catch (DataAccessException ex) {
            log.debug("Redis phone-lock unavailable, proceeding without lock: {}", ex.getMessage());
            return false;
        }
    }

    private void releasePhoneLockAfterCommit(String phoneLookupHash) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        Runnable release = () -> {
            try {
                redisTemplate.delete(phoneLockKey(phoneLookupHash));
            } catch (DataAccessException ex) {
                log.debug("Redis phone-lock release failed (will expire via TTL): {}", ex.getMessage());
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            release.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                release.run();
            }
        });
    }

    private String phoneLockKey(String phoneLookupHash) {
        return PHONE_LOCK_KEY_PREFIX + phoneLookupHash;
    }
}
