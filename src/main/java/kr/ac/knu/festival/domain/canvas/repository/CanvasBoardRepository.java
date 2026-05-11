package kr.ac.knu.festival.domain.canvas.repository;

import kr.ac.knu.festival.domain.canvas.entity.CanvasBoard;
import kr.ac.knu.festival.domain.canvas.entity.CanvasBoardQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CanvasBoardRepository extends JpaRepository<CanvasBoard, Long> {

    @Query("SELECT b, COUNT(p) FROM CanvasBoard b LEFT JOIN CanvasPostit p ON p.board = b WHERE b.question.id = :questionId GROUP BY b ORDER BY b.id ASC")
    List<Object[]> findBoardSummariesByQuestion(@Param("questionId") Long questionId);
}
