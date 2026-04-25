package kr.ac.knu.festival.domain.booth.repository;

import kr.ac.knu.festival.domain.booth.entity.Booth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoothRepository extends JpaRepository<Booth, Long> {

    List<Booth> findAllByOrderByLikeCountDescIdAsc();
}
