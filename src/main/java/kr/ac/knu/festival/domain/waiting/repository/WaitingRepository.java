package kr.ac.knu.festival.domain.waiting.repository;

import kr.ac.knu.festival.domain.waiting.entity.Waiting;
import kr.ac.knu.festival.domain.waiting.entity.WaitingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WaitingRepository extends JpaRepository<Waiting, Long> {

    @Query("""
            SELECT w FROM Waiting w
            JOIN FETCH w.booth
            WHERE w.booth.id = :boothId AND w.status IN :statuses
            ORDER BY w.sortOrder ASC
            """)
    List<Waiting> findAllByBoothIdAndStatusInOrderBySortOrderAsc(
            @Param("boothId") Long boothId,
            @Param("statuses") List<WaitingStatus> statuses
    );

    @Query("""
            SELECT w FROM Waiting w
            JOIN FETCH w.booth
            WHERE w.booth.id = :boothId
            ORDER BY w.sortOrder ASC
            """)
    List<Waiting> findAllByBoothIdOrderBySortOrderAsc(@Param("boothId") Long boothId);

    long countByBoothIdAndStatusIn(Long boothId, List<WaitingStatus> statuses);

    /**
     * H1: 모든 부스의 활성 대기 수를 한 번의 GROUP BY 쿼리로 집계.
     */
    @Query("""
            SELECT w.booth.id, COUNT(w) FROM Waiting w
            WHERE w.status IN :statuses
            GROUP BY w.booth.id
            """)
    List<Object[]> countActiveByBooth(@Param("statuses") List<WaitingStatus> statuses);

    @Query("""
            SELECT w.booth.id, COUNT(w) FROM Waiting w
            WHERE w.status = kr.ac.knu.festival.domain.waiting.entity.WaitingStatus.CALLED
              AND w.calledAt < :threshold
            GROUP BY w.booth.id
            """)
    List<Object[]> countExpiredCallsByBooth(@Param("threshold") LocalDateTime threshold);

    @Query("""
            SELECT w FROM Waiting w
            WHERE w.booth.id = :boothId
              AND w.phoneLookupHash = :phoneLookupHash
              AND w.status IN :statuses
            """)
    Optional<Waiting> findFirstActiveByBoothAndPhoneLookupHash(
            @Param("boothId") Long boothId,
            @Param("phoneLookupHash") String phoneLookupHash,
            @Param("statuses") List<WaitingStatus> statuses
    );

    long countByPhoneLookupHashAndStatusIn(String phoneLookupHash, List<WaitingStatus> statuses);

    @Query("SELECT DISTINCT w.name FROM Waiting w WHERE w.phoneLookupHash = :hash AND w.status IN :statuses")
    List<String> findDistinctNamesByPhoneLookupHashAndStatusIn(
            @Param("hash") String hash,
            @Param("statuses") List<WaitingStatus> statuses
    );

    @Query("""
            SELECT w FROM Waiting w JOIN FETCH w.booth
            WHERE w.phoneLookupHash = :hash AND w.status IN :statuses AND w.booth.id != :excludeBoothId
            """)
    List<Waiting> findActiveByPhoneLookupHashExcludingBooth(
            @Param("hash") String hash,
            @Param("statuses") List<WaitingStatus> statuses,
            @Param("excludeBoothId") Long excludeBoothId
    );

    @Query("""
            SELECT w FROM Waiting w JOIN FETCH w.booth
            WHERE w.phoneLookupHash = :hash AND w.status IN :statuses
            ORDER BY w.createdAt DESC
            """)
    List<Waiting> findAllActiveByPhoneLookupHash(
            @Param("hash") String hash,
            @Param("statuses") List<WaitingStatus> statuses
    );

    @Query("""
            SELECT w FROM Waiting w JOIN FETCH w.booth
            WHERE w.status = kr.ac.knu.festival.domain.waiting.entity.WaitingStatus.CALLED
              AND w.calledAt < :threshold
            """)
    List<Waiting> findExpiredCalls(@Param("threshold") LocalDateTime threshold);

    /**
     * AutoSkip UPDATE 이후 실제 SKIPPED 로 전이된 행만 다시 조회해
     * SMS 발송 대상에서 race-loser (ENTERED 등) 를 제외하기 위한 보조 메서드.
     */
    @Query("""
            SELECT w FROM Waiting w JOIN FETCH w.booth
            WHERE w.id IN :ids
              AND w.status = kr.ac.knu.festival.domain.waiting.entity.WaitingStatus.SKIPPED
            """)
    List<Waiting> findSkippedByIds(@Param("ids") List<Long> ids);

    @Query("SELECT COALESCE(MAX(w.waitingNumber), 0) FROM Waiting w WHERE w.booth.id = :boothId")
    int findMaxWaitingNumberByBoothId(@Param("boothId") Long boothId);

    @Query("SELECT COALESCE(MAX(w.sortOrder), 0) FROM Waiting w WHERE w.booth.id = :boothId")
    int findMaxSortOrderByBoothId(@Param("boothId") Long boothId);

    /**
     * M4: 10분 미방문 대기를 단일 UPDATE 로 일괄 SKIP.
     * 다른 트랜잭션이 ENTER 처리하면서 status 를 변경했다면 affected rows 가 0 이 되어
     * @Version 우회 race 를 방지한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Waiting w
            SET w.status = kr.ac.knu.festival.domain.waiting.entity.WaitingStatus.SKIPPED,
                w.version = w.version + 1
            WHERE w.id IN :ids
              AND w.status = kr.ac.knu.festival.domain.waiting.entity.WaitingStatus.CALLED
            """)
    int skipExpiredCalls(@Param("ids") List<Long> ids);

    /**
     * 중간 삽입 시 sort_order 일괄 시프트.
     * 호출 측에서 부스 행 PESSIMISTIC_WRITE 락을 잡은 상태로 호출해야 안전하다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Waiting w
            SET w.sortOrder = w.sortOrder + 1
            WHERE w.booth.id = :boothId AND w.sortOrder > :pivot
            """)
    int shiftSortOrdersUp(@Param("boothId") Long boothId, @Param("pivot") int pivot);
}
