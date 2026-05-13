package kr.ac.knu.festival.domain.canvas.repository;

import kr.ac.knu.festival.domain.canvas.entity.CanvasBoard;
import kr.ac.knu.festival.domain.canvas.entity.CanvasPostit;
import kr.ac.knu.festival.domain.canvas.entity.ModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanvasPostitRepository extends JpaRepository<CanvasPostit, Long> {

    List<CanvasPostit> findAllByBoardAndModerationStatusOrderByIdAsc(CanvasBoard board, ModerationStatus status);

    List<CanvasPostit> findAllByBoardAndModerationStatusNotOrderByIdAsc(CanvasBoard board, ModerationStatus status);

    long countByBoardAndModerationStatusNot(CanvasBoard board, ModerationStatus status);
}
