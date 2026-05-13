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
@Table(name = "canvas_board_question")
public class CanvasBoardQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "canvas_board_question_id")
    private Long id;

    @Column(nullable = false)
    private String content;

    @Column
    private String description;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "board_variant", nullable = false)
    private int boardVariant;

    public static CanvasBoardQuestion create(String content, String description, int orderIndex, int boardVariant) {
        return CanvasBoardQuestion.builder()
                .content(content)
                .description(description)
                .orderIndex(orderIndex)
                .boardVariant(boardVariant)
                .build();
    }
}
