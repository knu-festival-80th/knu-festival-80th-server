package kr.ac.knu.festival.domain.canvas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        @Index(name = "idx_canvas_postit_board_id", columnList = "canvas_board_id, canvas_postit_id")
})
@SQLDelete(sql = "UPDATE canvas_postit SET deleted_at = CURRENT_TIMESTAMP WHERE canvas_postit_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class CanvasPostit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "canvas_postit_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canvas_board_id", nullable = false)
    private CanvasBoard board;

    @Column(nullable = false, length = 60)
    private String message;

    @Column(name = "color_id", nullable = false)
    private int colorId;

    /** 보드 기준 0~100 상대좌표 (중심점 x). */
    @Column(name = "position_x", nullable = false)
    private double positionX;

    /** 보드 기준 0~100 상대좌표 (중심점 y). */
    @Column(name = "position_y", nullable = false)
    private double positionY;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static CanvasPostit createCanvasPostit(
            CanvasBoard board,
            int colorId,
            String message,
            double positionX,
            double positionY
    ) {
        return CanvasPostit.builder()
                .board(board)
                .colorId(colorId)
                .message(message)
                .positionX(positionX)
                .positionY(positionY)
                .build();
    }

    public void adjustPosition(double x, double y) {
        this.positionX = x;
        this.positionY = y;
    }
}
