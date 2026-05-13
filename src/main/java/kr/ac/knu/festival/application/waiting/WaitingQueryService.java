package kr.ac.knu.festival.application.waiting;

import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.domain.booth.repository.BoothRepository;
import kr.ac.knu.festival.domain.waiting.entity.Waiting;
import kr.ac.knu.festival.domain.waiting.entity.WaitingStatus;
import kr.ac.knu.festival.domain.waiting.repository.WaitingRepository;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import kr.ac.knu.festival.infra.security.PhoneLookupHasher;
import kr.ac.knu.festival.infra.security.PhoneNumberEncryptor;
import kr.ac.knu.festival.presentation.waiting.dto.response.MyWaitingResponse;
import kr.ac.knu.festival.presentation.waiting.dto.response.WaitingResponse;
import kr.ac.knu.festival.presentation.waiting.dto.response.WaitingStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WaitingQueryService {

    private static final List<WaitingStatus> ACTIVE_STATUSES = List.of(WaitingStatus.WAITING, WaitingStatus.CALLED);
    private static final int MINUTES_PER_TEAM = 5;

    private final BoothRepository boothRepository;
    private final WaitingRepository waitingRepository;
    private final PhoneNumberEncryptor phoneNumberEncryptor;
    private final PhoneLookupHasher phoneLookupHasher;

    public WaitingStatusResponse getBoothStatus(Long boothId) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND));
        long activeTeams = waitingRepository.countByBoothIdAndStatusIn(boothId, ACTIVE_STATUSES);
        int estimatedWaitMinutes = (int) activeTeams * MINUTES_PER_TEAM;
        return WaitingStatusResponse.of(boothId, booth.isWaitingOpen(), activeTeams, estimatedWaitMinutes);
    }

    public MyWaitingResponse getMyWaiting(Long waitingId, String last4Digits) {
        Waiting waiting = waitingRepository.findById(waitingId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.WAITING_NOT_FOUND));
        if (!phoneNumberEncryptor.matchesLast4(waiting.getPhoneNumber(), last4Digits)) {
            throw new BusinessException(BusinessErrorCode.PHONE_VERIFICATION_FAILED);
        }
        int aheadCount = countAheadOf(waiting);
        int estimatedWaitMinutes = aheadCount * MINUTES_PER_TEAM;
        return MyWaitingResponse.of(waiting, aheadCount, estimatedWaitMinutes);
    }

    public List<WaitingResponse> getWaitings(Long boothId, WaitingStatus status) {
        if (!boothRepository.existsById(boothId)) {
            throw new BusinessException(BusinessErrorCode.BOOTH_NOT_FOUND);
        }
        List<Waiting> waitings = (status == null)
                ? waitingRepository.findAllByBoothIdOrderBySortOrderAsc(boothId)
                : waitingRepository.findAllByBoothIdAndStatusInOrderBySortOrderAsc(boothId, List.of(status));
        return waitings.stream()
                .map(w -> WaitingResponse.fromEntity(w, maskPhone(w.getPhoneNumber())))
                .toList();
    }

    public List<MyWaitingResponse> getMyWaitings(String name, String phoneNumber) {
        String normalizedPhone = phoneNumber.replaceAll("\\D", "");
        String lookupHash = phoneLookupHasher.hash(normalizedPhone);

        List<Waiting> waitings = waitingRepository.findAllActiveByPhoneLookupHash(lookupHash, ACTIVE_STATUSES);
        if (waitings.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.WAITING_NOT_FOUND);
        }

        boolean nameMatches = waitings.stream().anyMatch(w -> w.getName().equals(name));
        if (!nameMatches) {
            throw new BusinessException(BusinessErrorCode.PHONE_VERIFICATION_FAILED);
        }

        return waitings.stream()
                .map(w -> MyWaitingResponse.of(w, countAheadOf(w), countAheadOf(w) * MINUTES_PER_TEAM))
                .toList();
    }

    private int countAheadOf(Waiting target) {
        if (target.getStatus() != WaitingStatus.WAITING) {
            return 0;
        }
        List<Waiting> active = waitingRepository.findAllByBoothIdAndStatusInOrderBySortOrderAsc(
                target.getBooth().getId(), ACTIVE_STATUSES);
        int count = 0;
        for (Waiting w : active) {
            if (w.getId().equals(target.getId())) {
                break;
            }
            count++;
        }
        return count;
    }

    private String maskPhone(String encryptedPhone) {
        String plain = phoneNumberEncryptor.decrypt(encryptedPhone);
        if (plain == null || plain.length() < 4) {
            return "****";
        }
        String digits = plain.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "****";
        }
        return "***-****-" + digits.substring(digits.length() - 4);
    }
}
