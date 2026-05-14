package kr.ac.knu.festival.domain.matching.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.ac.knu.festival.global.base.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "matching_service_state")
public class MatchingServiceState extends BaseTimeEntity {

    // 서비스 전체의 OPEN/PAUSED 상태를 저장하는 단일 행 ID.
    public static final long SINGLETON_ID = 1L;

    @Id
    @Column(name = "state_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchingOperationStatus status;

    public static MatchingServiceState defaultOpen() {
        return MatchingServiceState.builder()
                .id(SINGLETON_ID)
                .status(MatchingOperationStatus.OPEN)
                .build();
    }

    public void changeStatus(MatchingOperationStatus status) {
        this.status = status;
    }
}
