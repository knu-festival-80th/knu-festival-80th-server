package kr.ac.knu.festival.domain.canvas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "canvas_board")
public class CanvasBoard extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "canvas_board_id")
    private Long id;

    /** boardId가 홀수면 1번 디자인, 짝수면 2번 디자인. */
    public int getBoardVariant() {
        return (id % 2 == 1) ? 1 : 2;
    }

    public static CanvasBoard create() {
        return CanvasBoard.builder().build();
    }
}
