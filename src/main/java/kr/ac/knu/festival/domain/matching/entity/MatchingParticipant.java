package kr.ac.knu.festival.domain.matching.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.ac.knu.festival.global.base.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "matching_participant",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_matching_participant_id_day",
                        columnNames = {"instagram_id", "festival_day"}
                )
        },
        indexes = {
                @Index(name = "idx_matching_day_status_gender", columnList = "festival_day, status, gender")
        }
)
public class MatchingParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matching_participant_id")
    private Long id;

    @Column(name = "instagram_id", nullable = false, length = 100)
    private String instagramId;

    @Column(name = "festival_day", nullable = false)
    private LocalDate festivalDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MatchingGender gender;

    @Column(name = "phone_lookup_hash", nullable = false, length = 100)
    private String phoneLookupHash;

    @Column(name = "phone_encrypted", nullable = false, length = 255)
    private String phoneEncrypted;

    @Column(name = "matched_id", length = 100)
    private String matchedId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchingParticipantStatus status;

    public static String normalizeInstagramId(String instagramId) {
        return instagramId.trim().replaceFirst("^@", "").toLowerCase();
    }

    public static MatchingParticipant create(
            String instagramId,
            LocalDate festivalDay,
            MatchingGender gender,
            String phoneLookupHash,
            String phoneEncrypted
    ) {
        return MatchingParticipant.builder()
                .instagramId(instagramId)
                .festivalDay(festivalDay)
                .gender(gender)
                .phoneLookupHash(phoneLookupHash)
                .phoneEncrypted(phoneEncrypted)
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

    public void resetToPending() {
        this.matchedId = null;
        this.status = MatchingParticipantStatus.PENDING;
    }
}
