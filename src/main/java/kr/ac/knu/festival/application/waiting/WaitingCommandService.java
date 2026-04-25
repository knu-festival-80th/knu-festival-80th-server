package kr.ac.knu.festival.application.waiting;

import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.domain.booth.repository.BoothRepository;
import kr.ac.knu.festival.domain.waiting.entity.Waiting;
import kr.ac.knu.festival.domain.waiting.entity.WaitingStatus;
import kr.ac.knu.festival.domain.waiting.repository.WaitingRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.infra.security.PhoneNumberEncryptor;
import kr.ac.knu.festival.infra.sms.SmsSender;
import kr.ac.knu.festival.presentation.waiting.dto.request.WaitingCreateRequest;
import kr.ac.knu.festival.presentation.waiting.dto.request.WaitingInsertRequest;
import kr.ac.knu.festival.presentation.waiting.dto.request.WaitingReorderRequest;
import kr.ac.knu.festival.presentation.waiting.dto.response.WaitingRegisterResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WaitingCommandService {

    private static final List<WaitingStatus> ACTIVE_STATUSES = List.of(WaitingStatus.WAITING, WaitingStatus.CALLED);
    private static final int MINUTES_PER_TEAM = 5;

    private final BoothRepository boothRepository;
    private final WaitingRepository waitingRepository;
    private final PhoneNumberEncryptor phoneNumberEncryptor;
    private final SmsSender smsSender;

    private final ExecutorService smsExecutor = Executors.newFixedThreadPool(4);

    public WaitingRegisterResponse registerWaiting(Long boothId, WaitingCreateRequest request) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        if (!booth.isWaitingOpen()) {
            throw new BusinessException(BusinessErrorCode.WAITING_REGISTRATION_CLOSED);
        }

        String normalizedPhone = normalizePhoneNumber(request.phoneNumber());
        String encryptedPhone = phoneNumberEncryptor.encrypt(normalizedPhone);

        waitingRepository.findFirstByBoothIdAndPhoneNumberAndStatusIn(boothId, encryptedPhone, ACTIVE_STATUSES)
                .ifPresent(existing -> {
                    throw new BusinessException(BusinessErrorCode.DUPLICATE_WAITING);
                });

        int nextNumber = waitingRepository.findMaxWaitingNumberByBoothId(boothId) + 1;
        int nextSortOrder = waitingRepository.findMaxSortOrderByBoothId(boothId) + 1;

        Waiting waiting = Waiting.createWaiting(
                booth,
                nextNumber,
                nextSortOrder,
                request.name(),
                request.partySize(),
                encryptedPhone
        );
        Waiting saved = waitingRepository.save(waiting);

        long currentWaitingTeams = waitingRepository.countByBoothIdAndStatusIn(boothId, ACTIVE_STATUSES);
        int estimatedWaitMinutes = Math.max(0, (int) (currentWaitingTeams - 1)) * MINUTES_PER_TEAM;
        return WaitingRegisterResponse.of(saved, booth, currentWaitingTeams, estimatedWaitMinutes);
    }

    public void callWaiting(Long waitingId) {
        Waiting waiting = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.WAITING_NOT_FOUND));
        waiting.markCalled();
        sendCallSmsAsync(waiting.getId(), waiting.getPhoneNumber(), waiting.getBooth().getName(), waiting.getWaitingNumber());
    }

    public void enterWaiting(Long waitingId) {
        Waiting waiting = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.WAITING_NOT_FOUND));
        waiting.markEntered();
    }

    public void cancelWaiting(Long waitingId) {
        Waiting waiting = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.WAITING_NOT_FOUND));
        waiting.markCancelled();
    }

    public void skipWaiting(Long waitingId) {
        Waiting waiting = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.WAITING_NOT_FOUND));
        waiting.markSkipped();
    }

    public WaitingRegisterResponse insertWaiting(Long boothId, WaitingInsertRequest request) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));

        String normalizedPhone = normalizePhoneNumber(request.phoneNumber());
        String encryptedPhone = phoneNumberEncryptor.encrypt(normalizedPhone);

        waitingRepository.findFirstByBoothIdAndPhoneNumberAndStatusIn(boothId, encryptedPhone, ACTIVE_STATUSES)
                .ifPresent(existing -> {
                    throw new BusinessException(BusinessErrorCode.DUPLICATE_WAITING);
                });

        List<Waiting> all = waitingRepository.findAllByBoothIdOrderBySortOrderAsc(boothId);
        int insertAt = request.insertAfterSortOrder();
        for (Waiting w : all) {
            if (w.getSortOrder() > insertAt) {
                w.updateSortOrder(w.getSortOrder() + 1);
            }
        }

        int nextNumber = waitingRepository.findMaxWaitingNumberByBoothId(boothId) + 1;
        Waiting waiting = Waiting.createWaiting(
                booth,
                nextNumber,
                insertAt + 1,
                request.name(),
                request.partySize(),
                encryptedPhone
        );
        Waiting saved = waitingRepository.save(waiting);

        long currentWaitingTeams = waitingRepository.countByBoothIdAndStatusIn(boothId, ACTIVE_STATUSES);
        int estimatedWaitMinutes = Math.max(0, (int) (currentWaitingTeams - 1)) * MINUTES_PER_TEAM;
        return WaitingRegisterResponse.of(saved, booth, currentWaitingTeams, estimatedWaitMinutes);
    }

    public void reorderWaiting(Long waitingId, WaitingReorderRequest request) {
        Waiting target = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.WAITING_NOT_FOUND));
        Long boothId = target.getBooth().getId();
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
        if (waiting.getStatus() != WaitingStatus.CALLED) {
            throw new BusinessException(BusinessErrorCode.INVALID_WAITING_STATUS_TRANSITION);
        }
        waiting.markSmsFailed();
        sendCallSmsAsync(waiting.getId(), waiting.getPhoneNumber(), waiting.getBooth().getName(), waiting.getWaitingNumber());
    }

    private void sendCallSmsAsync(Long waitingId, String encryptedPhone, String boothName, int waitingNumber) {
        String plainPhone = phoneNumberEncryptor.decrypt(encryptedPhone);
        if (plainPhone == null) {
            log.error("Phone decrypt failed for waiting {}", waitingId);
            return;
        }
        String message = "[%s] %d번 손님, 입장 차례입니다. 5분 내 도착 부탁드립니다.".formatted(boothName, waitingNumber);
        CompletableFuture.runAsync(() -> {
            boolean success = smsSender.send(plainPhone, message);
            if (success) {
                markSmsSent(waitingId);
            }
        }, smsExecutor).exceptionally(ex -> {
            log.warn("SMS dispatch failed for waiting {}: {}", waitingId, ex.getMessage());
            return null;
        });
    }

    @Transactional
    public void markSmsSent(Long waitingId) {
        waitingRepository.findById(waitingId).ifPresent(Waiting::markSmsSent);
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_PHONE_NUMBER);
        }
        return phoneNumber.replaceAll("\\D", "");
    }
}
