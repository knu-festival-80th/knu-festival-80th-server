package kr.ac.knu.festival.domain.matching.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(
        name = "matching_participant",
        indexes = {
                @Index(name = "idx_matching_status_gender", columnList = "status, gender")
        }
)
public class MatchingParticipant extends BaseTimeEntity {

    @Id
    // 축제 기간 중 1회 참여 제한을 DB 차원에서도 보장하기 위해 Instagram ID를 PK로 사용한다.
    @Column(name = "instagram_id", length = 100)
    private String instagramId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MatchingGender gender;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 10)
    private String nationality;

    @Column(name = "matched_id", length = 100)
    private String matchedId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchingParticipantStatus status;

    public static MatchingParticipant create(
            String instagramId,
            MatchingGender gender,
            String encodedPassword,
            String nationality
    ) {
        return MatchingParticipant.builder()
                .instagramId(instagramId)
                .gender(gender)
                .password(encodedPassword)
                .nationality(nationality)
                .status(MatchingParticipantStatus.PENDING)
                .build();
    }

    public void matchWith(String matchedId) {
        this.matchedId = matchedId;
        this.status = MatchingParticipantStatus.MATCHED;
    }

    public void markUnmatched() {
        this.matchedId = null;
        this.status = MatchingParticipantStatus.UNMATCHED;
    }

    public void cancel() {
        this.status = MatchingParticipantStatus.CANCELLED;
    }
}
