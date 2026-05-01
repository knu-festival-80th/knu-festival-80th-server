package kr.ac.knu.festival.domain.matching.repository;

import kr.ac.knu.festival.domain.matching.entity.MatchingGender;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipant;
import kr.ac.knu.festival.domain.matching.entity.MatchingParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;

public interface MatchingParticipantRepository extends JpaRepository<MatchingParticipant, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p FROM MatchingParticipant p
            WHERE p.status = :status AND p.gender = :gender
            """)
    List<MatchingParticipant> findAllByStatusAndGenderForUpdate(
            @Param("status") MatchingParticipantStatus status,
            @Param("gender") MatchingGender gender
    );

    List<MatchingParticipant> findAllByStatus(MatchingParticipantStatus status);

    long countByStatusAndGender(MatchingParticipantStatus status, MatchingGender gender);
}
