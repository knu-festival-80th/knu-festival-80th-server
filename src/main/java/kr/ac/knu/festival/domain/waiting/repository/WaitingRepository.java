package kr.ac.knu.festival.domain.waiting.repository;

import kr.ac.knu.festival.domain.waiting.entity.Waiting;
import kr.ac.knu.festival.domain.waiting.entity.WaitingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WaitingRepository extends JpaRepository<Waiting, Long> {

    List<Waiting> findAllByBoothIdAndStatusInOrderBySortOrderAsc(Long boothId, List<WaitingStatus> statuses);

    List<Waiting> findAllByBoothIdOrderBySortOrderAsc(Long boothId);

    long countByBoothIdAndStatusIn(Long boothId, List<WaitingStatus> statuses);

    Optional<Waiting> findFirstByBoothIdAndPhoneNumberAndStatusIn(Long boothId, String phoneNumber, List<WaitingStatus> statuses);

    @Query("select coalesce(max(w.waitingNumber), 0) from Waiting w where w.booth.id = :boothId")
    int findMaxWaitingNumberByBoothId(@Param("boothId") Long boothId);

    @Query("select coalesce(max(w.sortOrder), 0) from Waiting w where w.booth.id = :boothId")
    int findMaxSortOrderByBoothId(@Param("boothId") Long boothId);

    List<Waiting> findAllByStatusAndCalledAtBefore(WaitingStatus status, LocalDateTime threshold);
}
