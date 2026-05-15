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

    /**
     * 그 날 신청 누적 (status 무관) 카운트. 결과창 동안에도 PENDING→MATCHED/UNMATCHED 전환에 영향받지 않고
     * 신청자 수를 유지한다. 다음 신청창이 열리는 다음날 11시에 currentDayForCounts 가 새 날짜로 전환되면 자연스럽게 0.
     */
    long countByFestivalDayAndGender(LocalDate festivalDay, MatchingGender gender);

    // 단일 GROUP BY 쿼리로 PENDING/MATCHED/UNMATCHED + 성별 PENDING 카운트를 한 번에 집계한다.
    @Query("""
            SELECT p.status, p.gender, COUNT(p)
            FROM MatchingParticipant p
            WHERE p.festivalDay = :day
            GROUP BY p.status, p.gender
            """)
    List<Object[]> countByDayGroupByStatusGender(@Param("day") LocalDate day);

    @Query("""
            SELECT p FROM MatchingParticipant p
            WHERE p.festivalDay = :festivalDay
              AND (:status IS NULL OR p.status = :status)
              AND (:gender IS NULL OR p.gender = :gender)
              AND (:search IS NULL OR p.instagramId LIKE CONCAT(:search, '%'))
            ORDER BY p.createdAt DESC
            """)
    List<MatchingParticipant> searchForAdmin(
            @Param("festivalDay") LocalDate festivalDay,
            @Param("status") MatchingParticipantStatus status,
            @Param("gender") MatchingGender gender,
            @Param("search") String search
    );
}
