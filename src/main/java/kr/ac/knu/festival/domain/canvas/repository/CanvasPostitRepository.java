package kr.ac.knu.festival.domain.canvas.repository;

import kr.ac.knu.festival.domain.canvas.entity.CanvasPostit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CanvasPostitRepository extends JpaRepository<CanvasPostit, Long> {

    List<CanvasPostit> findAllByZoneNumberOrderByIdAsc(int zoneNumber);

    @Query("SELECT c.zoneNumber, COUNT(c) FROM CanvasPostit c GROUP BY c.zoneNumber ORDER BY c.zoneNumber ASC")
    List<Object[]> countByZone();

    @Query("SELECT COALESCE(MAX(c.zoneNumber), 1) FROM CanvasPostit c")
    int findMaxZoneNumber();
}
