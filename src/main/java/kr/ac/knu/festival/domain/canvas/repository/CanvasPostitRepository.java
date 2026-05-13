package kr.ac.knu.festival.domain.canvas.repository;

import kr.ac.knu.festival.domain.canvas.entity.CanvasBoard;
import kr.ac.knu.festival.domain.canvas.entity.CanvasPostit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanvasPostitRepository extends JpaRepository<CanvasPostit, Long> {

    List<CanvasPostit> findAllByBoardOrderByIdAsc(CanvasBoard board);

    long countByBoard(CanvasBoard board);
}
