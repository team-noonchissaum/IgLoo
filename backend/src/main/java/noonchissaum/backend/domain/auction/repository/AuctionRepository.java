package noonchissaum.backend.domain.auction.repository;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> , JpaSpecificationExecutor<Auction> {

    @EntityGraph(attributePaths = {"item", "item.seller", "item.category"})
    Page<Auction> findAllByStatus(AuctionStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"item", "item.seller", "item.category"})
    Page<Auction> findAll(Pageable pageable);

    List<Auction> findAllByStatusIn(List<AuctionStatus> statuses);

    // Redis id 리스트 상세 로딩용
    @EntityGraph(attributePaths = {"item", "item.seller", "item.category"})
    List<Auction> findByIdIn(List<Long> ids);

    /**
     *스케줄 관련 상태값 변경쿼리
     * READY->RUNNING
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Auction a
    SET a.status = :toStatus,
        a.startAt = :now
    WHERE a.status = :fromStatus
      AND a.createdAt <= :threshold
""")
    int exposeReadyAuctions(
            @Param("fromStatus") AuctionStatus fromStatus,
            @Param("toStatus") AuctionStatus toStatus,
            @Param("threshold") LocalDateTime threshold,
            @Param("now") LocalDateTime now
    );

    List<Auction> findByStartAt(LocalDateTime startAt);

    /**
     *스케줄 관련 상태값 변경쿼리
     * deadline -> ended
     */
    @Modifying
    @Query("""
    update Auction a
    set a.status = :ended
    where a.status = :deadline
      and a.endAt <= :now
""")
    int endRunningAuctions(
            AuctionStatus deadline,
            AuctionStatus ended,
            LocalDateTime now
    );

    /**
     * 스케줄 관련 상태값 변경 쿼리
     * Running -> DEADLINE
     */
    @Modifying
    @Query(
            value = """
        UPDATE auctions a
        SET a.status = 'DEADLINE'
        WHERE a.status = 'RUNNING'
            AND TIMESTAMPDIFF(MINUTE, :now, a.end_at) <= a.imminent_minutes
        """,
            nativeQuery = true
    )
    int markDeadlineAuctions(@Param("now") LocalDateTime now);

    //deadline으로 바뀔 경매 찾기
    @Query(
            value = """
        SELECT a.auction_id
        FROM auctions a
        WHERE a.status = 'RUNNING'
          AND TIMESTAMPDIFF(MINUTE, :now, a.end_at) <= a.imminent_minutes
    """,
            nativeQuery = true
    )
    List<Long> findRunningAuctionsToDeadline(@Param("now") LocalDateTime now);


    @Query(
            "select a from Auction a where a.id = :auctionId " +
                    "and (a.status = noonchissaum.backend.domain.auction.entity.AuctionStatus.RUNNING " +
                    "or a.status = noonchissaum.backend.domain.auction.entity.AuctionStatus.DEADLINE)")
    Optional<Auction> findByIdWithStatus(@Param("auctionId") Long auctionId);

    @Modifying
    @Query("""
    update Auction a
    set a.status = :toStatus
    where a.id = :auctionId
    and a.status = :fromStatus
    """)
    int finalizeAuctionStatus(
            @Param("auctionId") Long auctionId,
            @Param("fromStatus") AuctionStatus fromStatus,
            @Param("toStatus") AuctionStatus toStatus
    );

}

