package kr.ac.knu.festival.domain.canvas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "canvas_postit", indexes = {
        @Index(name = "idx_canvas_postit_zone_id", columnList = "zone_number, canvas_postit_id")
})
@SQLDelete(sql = "UPDATE canvas_postit SET deleted_at = CURRENT_TIMESTAMP WHERE canvas_postit_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class CanvasPostit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "canvas_postit_id")
    private Long id;

    @Column(nullable = false, length = 8)
    private String nickname;

    @Column(nullable = false, length = 60)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PostitColor color;

    @Column(name = "position_x", nullable = false)
    private int positionX;

    @Column(name = "position_y", nullable = false)
    private int positionY;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "zone_number", nullable = false)
    private int zoneNumber;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static CanvasPostit createCanvasPostit(
            String nickname,
            String message,
            PostitColor color,
            int positionX,
            int positionY,
            int width,
            int height
    ) {
        return CanvasPostit.builder()
                .nickname(nickname)
                .message(message)
                .color(color)
                .positionX(positionX)
                .positionY(positionY)
                .width(width)
                .height(height)
                .zoneNumber(0)
                .build();
    }

    public void assignZone() {
        this.zoneNumber = (int) ((this.id - 1) / 50) + 1;
    }

    public void adjustPosition(int x, int y) {
        this.positionX = x;
        this.positionY = y;
    }
}