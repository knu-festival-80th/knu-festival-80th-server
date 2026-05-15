package kr.ac.knu.festival.domain.booth.repository;

import kr.ac.knu.festival.domain.booth.entity.MapLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapLocationRepository extends JpaRepository<MapLocation, Long> {
}
