package kr.ac.knu.festival.domain.canvas.repository;

import kr.ac.knu.festival.domain.canvas.entity.CanvasBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CanvasBoardRepository extends JpaRepository<CanvasBoard, Long> {

    @Query("SELECT b FROM CanvasBoard b ORDER BY b.id DESC LIMIT 1")
    Optional<CanvasBoard> findLatestBoard();

    @Query("SELECT b, COUNT(p) FROM CanvasBoard b LEFT JOIN CanvasPostit p ON p.board = b GROUP BY b ORDER BY b.id ASC")
    List<Object[]> findBoardSummaries();
}
