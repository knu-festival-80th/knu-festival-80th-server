package kr.ac.knu.festival.domain.canvas.repository;

import kr.ac.knu.festival.domain.canvas.entity.CanvasBoardQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanvasBoardQuestionRepository extends JpaRepository<CanvasBoardQuestion, Long> {

    List<CanvasBoardQuestion> findAllByOrderByOrderIndexAsc();
}
