package kr.ac.knu.festival.domain.waiting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import kr.ac.knu.festival.domain.booth.entity.Booth;
import kr.ac.knu.festival.global.base.BaseTimeEntity;
import kr.ac.knu.festival.global.exception.BusinessErrorCode;
import kr.ac.knu.festival.global.exception.BusinessException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "waiting",
        indexes = {
                @Index(name = "idx_waiting_booth_status", columnList = "booth_id, status"),
                @Index(name = "idx_waiting_booth_sort", columnList = "booth_id, sort_order"),
                @Index(name = "idx_waiting_booth_lookup_status", columnList = "booth_id, phone_lookup_hash, status"),
                @Index(name = "idx_waiting_status_called_at", columnList = "status, called_at")
        }
)
@SQLDelete(sql = "UPDATE waiting SET deleted_at = CURRENT_TIMESTAMP WHERE waiting_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Waiting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "waiting_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_id", nullable = false)
    private Booth booth;

    @Column(name = "waiting_number", nullable = false)
    private int waitingNumber;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "party_size", nullable = false)
    private int partySize;

    @Column(name = "phone_number", nullable = false, length = 100)
    private String phoneNumber;

    @Column(name = "phone_lookup_hash", nullable = false, length = 64)
    private String phoneLookupHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitingStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "sms_sent", nullable = false)
    private boolean smsSent;

    @Column(name = "called_at")
    private LocalDateTime calledAt;

    @Column(name = "entered_at")
    private LocalDateTime enteredAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static Waiting createWaiting(
            Booth booth,
            int waitingNumber,
            int sortOrder,
            String name,
            int partySize,
            String phoneNumber,
            String phoneLookupHash
    ) {
        return Waiting.builder()
                .booth(booth)
                .waitingNumber(waitingNumber)
                .sortOrder(sortOrder)
                .name(name)
                .partySize(partySize)
                .phoneNumber(phoneNumber)
                .phoneLookupHash(phoneLookupHash)
                .status(WaitingStatus.WAITING)
                .smsSent(false)
                .build();
    }

    public void markCalled() {
        if (!status.canCall()) {
            throw new BusinessException(BusinessErrorCode.INVALID_WAITING_STATUS_TRANSITION);
        }
        this.status = WaitingStatus.CALLED;
        this.calledAt = LocalDateTime.now();
    }

    public void markEntered() {
        if (!status.canEnter()) {
            throw new BusinessException(BusinessErrorCode.INVALID_WAITING_STATUS_TRANSITION);
        }
        this.status = WaitingStatus.ENTERED;
        this.enteredAt = LocalDateTime.now();
    }

    public void markSkipped() {
        if (!status.canSkip()) {
            throw new BusinessException(BusinessErrorCode.INVALID_WAITING_STATUS_TRANSITION);
        }
        this.status = WaitingStatus.SKIPPED;
    }

    public void markCancelled() {
        if (!status.canCancel()) {
            throw new BusinessException(BusinessErrorCode.INVALID_WAITING_STATUS_TRANSITION);
        }
        this.status = WaitingStatus.CANCELLED;
    }

    public void markSmsSent() {
        this.smsSent = true;
    }

    public void markSmsFailed() {
        this.smsSent = false;
    }

    public void updateSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
