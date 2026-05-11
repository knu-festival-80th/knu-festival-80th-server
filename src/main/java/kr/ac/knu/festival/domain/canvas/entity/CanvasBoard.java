package kr.ac.knu.festival.domain.canvas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canvas_board_question_id", nullable = false)
    private CanvasBoardQuestion question;

    @Column(name = "board_variant", nullable = false)
    private int boardVariant;

    @Column(name = "max_note_count", nullable = false)
    private int maxNoteCount;

    public static CanvasBoard create(CanvasBoardQuestion question, int boardVariant, int maxNoteCount) {
        return CanvasBoard.builder()
                .question(question)
                .boardVariant(boardVariant)
                .maxNoteCount(maxNoteCount)
                .build();
    }
}
