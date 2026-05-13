package kr.ac.knu.festival.domain.canvas.repository;

import jakarta.persistence.LockModeType;
import kr.ac.knu.festival.domain.canvas.entity.CanvasBoard;
import kr.ac.knu.festival.domain.canvas.entity.CanvasBoardQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CanvasBoardRepository extends JpaRepository<CanvasBoard, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM CanvasBoard b WHERE b.id = :id")
    Optional<CanvasBoard> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT b, COUNT(p) FROM CanvasBoard b LEFT JOIN CanvasPostit p ON p.board = b AND p.moderationStatus = kr.ac.knu.festival.domain.canvas.entity.ModerationStatus.APPROVED WHERE b.question.id = :questionId GROUP BY b ORDER BY b.id ASC")
    List<Object[]> findBoardSummariesByQuestion(@Param("questionId") Long questionId);
}
