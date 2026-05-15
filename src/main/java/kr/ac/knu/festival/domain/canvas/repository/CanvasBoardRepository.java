package kr.ac.knu.festival.domain.canvas.repository;

import jakarta.persistence.LockModeType;
import kr.ac.knu.festival.domain.canvas.entity.CanvasBoard;
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

    /**
     * 보드별 승인된 포스트잇 개수 집계.
     * b.question 을 JOIN FETCH 해 후속 getQuestion().getBoardVariant() 접근 시의 N+1 을 방지한다.
     */
    @Query("""
            SELECT b, COUNT(p)
            FROM CanvasBoard b
            JOIN FETCH b.question q
            LEFT JOIN CanvasPostit p
                ON p.board = b
                AND p.moderationStatus = kr.ac.knu.festival.domain.canvas.entity.ModerationStatus.APPROVED
            WHERE q.id = :questionId
            GROUP BY b, q
            ORDER BY b.id ASC
            """)
    List<Object[]> findBoardSummariesByQuestion(@Param("questionId") Long questionId);
}
