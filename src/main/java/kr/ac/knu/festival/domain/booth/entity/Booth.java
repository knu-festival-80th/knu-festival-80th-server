package kr.ac.knu.festival.domain.booth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
        @Index(name = "idx_booth_like_count", columnList = "like_count"),
        @Index(name = "idx_booth_total_waiting_count", columnList = "total_waiting_count")
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_location_id")
    private MapLocation mapLocation;

    @Column(length = 100)
    private String department;


    @Column(length = 200)
    private String location;



    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "total_waiting_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int totalWaitingCount;

    @Column(name = "menu_board_image_url", length = 500)
    private String menuBoardImageUrl;

    @Column(name = "is_waiting_open", nullable = false)
    private boolean waitingOpen;

    @Column(name = "admin_password", length = 255)
    private String adminPassword;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static Booth createBooth(
            String name,
            MapLocation mapLocation,
            String menuBoardImageUrl,
            String encodedAdminPassword,
            String department,
            String location
    ) {
        return Booth.builder()
                .name(name)
                .mapLocation(mapLocation)
                .menuBoardImageUrl(menuBoardImageUrl)
                .likeCount(0)
                .totalWaitingCount(0)
                .waitingOpen(false)
                .adminPassword(encodedAdminPassword)
                .department(department)
                .location(location)
                .build();
    }

    public void updateBooth(
            String name,
            String menuBoardImageUrl,
            String department,
            String location
    ) {
        if (name != null && !name.isBlank()) this.name = name;
        if (menuBoardImageUrl != null) this.menuBoardImageUrl = menuBoardImageUrl;
        if (department != null) this.department = department;
        if (location != null) this.location = location;
    }

    public BigDecimal getXRatio() {
        return mapLocation != null ? mapLocation.getXRatio() : null;
    }

    public BigDecimal getYRatio() {
        return mapLocation != null ? mapLocation.getYRatio() : null;
    }

    private static final long TAVERN_MAX_ID = 38;

    public MapLocationType getMapLocationType() {
        if (mapLocation != null) return mapLocation.getType();
        return id != null && id <= TAVERN_MAX_ID ? MapLocationType.TAVERN : MapLocationType.BOOTH;
    }

    public void assignMapLocation(MapLocation mapLocation) {
        this.mapLocation = mapLocation;
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

    public void changeAdminPassword(String encodedPassword) {
        this.adminPassword = encodedPassword;
    }
}
