package kr.ac.knu.festival.domain.booth.repository;

import kr.ac.knu.festival.domain.booth.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findAllByBoothIdOrderByIdAsc(Long boothId);

    java.util.Optional<Menu> findByIdAndBoothId(Long id, Long boothId);
}
