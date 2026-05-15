package kr.ac.knu.festival.domain.matching.repository;

import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MatchingParticipantRepository extends JpaRepository<MatchingParticipant, Long> {

    boolean existsByInstagramIdAndFestivalDay(String instagramId, LocalDate festivalDay);

    boolean existsByPhoneLookupHashAndFestivalDay(String phoneLookupHash, LocalDate festivalDay);

    Optional<MatchingParticipant> findByInstagramIdAndFestivalDay(String instagramId, LocalDate festivalDay);

    // 자동 스케줄러와 관리자 수동 실행이 겹쳐도 같은 참가자를 두 번 매칭하지 않도록 행 락을 잡는다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p FROM MatchingParticipant p
            WHERE p.festivalDay = :festivalDay
              AND p.status = :status
              AND p.gender = :gender
            """)
    List<MatchingParticipant> findAllByDayAndStatusAndGenderForUpdate(
            @Param("festivalDay") LocalDate festivalDay,
            @Param("status") MatchingParticipantStatus status,
            @Param("gender") MatchingGender gender
    );

    List<MatchingParticipant> findAllByFestivalDayAndStatus(LocalDate festivalDay, MatchingParticipantStatus status);

    long countByFestivalDayAndStatus(LocalDate festivalDay, MatchingParticipantStatus status);

    long countByFestivalDayAndStatusAndGender(
            LocalDate festivalDay,
            MatchingParticipantStatus status,
            MatchingGender gender
    );

    @Query("""
            SELECT p FROM MatchingParticipant p
            WHERE p.festivalDay = :festivalDay
              AND (:status IS NULL OR p.status = :status)
              AND (:gender IS NULL OR p.gender = :gender)
              AND (:search IS NULL OR LOWER(p.instagramId) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY p.createdAt DESC
            """)
    List<MatchingParticipant> searchForAdmin(
            @Param("festivalDay") LocalDate festivalDay,
            @Param("status") MatchingParticipantStatus status,
            @Param("gender") MatchingGender gender,
            @Param("search") String search
    );
}
