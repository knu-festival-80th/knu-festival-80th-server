package kr.ac.knu.festival.domain.canvas.repository;

import kr.ac.knu.festival.domain.canvas.entity.CanvasBoard;
import kr.ac.knu.festival.domain.canvas.entity.CanvasPostit;
import kr.ac.knu.festival.domain.canvas.entity.ModerationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanvasPostitRepository extends JpaRepository<CanvasPostit, Long> {

    /**
     * 보드별 승인된 포스트잇 목록.
     * board, board.question 까지 함께 fetch 해 board.getQuestion() 접근 시의 N+1 을 방지한다.
     */
    @EntityGraph(attributePaths = {"board", "board.question"})
    List<CanvasPostit> findAllByBoardAndModerationStatusOrderByIdAsc(CanvasBoard board, ModerationStatus status);

    @EntityGraph(attributePaths = {"board", "board.question"})
    List<CanvasPostit> findAllByBoardAndModerationStatusNotOrderByIdAsc(CanvasBoard board, ModerationStatus status);

    long countByBoardAndModerationStatusNot(CanvasBoard board, ModerationStatus status);
}
