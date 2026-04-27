package kr.ac.knu.festival.domain.booth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kr.ac.knu.festival.global.base.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "booth", indexes = {
        @Index(name = "idx_booth_like_count", columnList = "like_count")
})
@SQLDelete(sql = "UPDATE booth SET deleted_at = CURRENT_TIMESTAMP WHERE booth_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Booth extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booth_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "location_lat", precision = 10, scale = 7)
    private BigDecimal locationLat;

    @Column(name = "location_lng", precision = 10, scale = 7)
    private BigDecimal locationLng;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_waiting_open", nullable = false)
    private boolean waitingOpen;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static Booth createBooth(
            String name,
            String description,
            BigDecimal locationLat,
            BigDecimal locationLng,
            String imageUrl
    ) {
        return Booth.builder()
                .name(name)
                .description(description)
                .locationLat(locationLat)
                .locationLng(locationLng)
                .imageUrl(imageUrl)
                .likeCount(0)
                .waitingOpen(true)
                .build();
    }

    public void updateBooth(
            String name,
            String description,
            BigDecimal locationLat,
            BigDecimal locationLng,
            String imageUrl
    ) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (locationLat != null) {
            this.locationLat = locationLat;
        }
        if (locationLng != null) {
            this.locationLng = locationLng;
        }
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
    }

    public void increaseLike() {
        this.likeCount++;
    }

    public void decreaseLike() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void toggleWaitingOpen(boolean waitingOpen) {
        this.waitingOpen = waitingOpen;
    }
}
