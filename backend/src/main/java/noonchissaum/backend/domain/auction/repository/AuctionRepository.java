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

    @EntityGraph(attributePaths = {"item", "item.seller", "item.category"})
    Page<Auction> findAllByItem_Seller_Id(Long sellerId, Pageable pageable);

    List<Auction> findAllByStatusIn(List<AuctionStatus> statuses);

    @EntityGraph(attributePaths = {"item", "item.seller", "item.category"})
    List<Auction> findByIdIn(List<Long> ids);

    /**
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    select  a
    from Auction a
    WHERE a.status = :fromStatus
      AND a.createdAt <= :threshold
""")
    Optional<List<Auction>> findReadyAuctions(
            @Param("fromStatus") AuctionStatus fromStatus,
            @Param("threshold") LocalDateTime threshold
    );

    List<Auction> findByStartAt(LocalDateTime startAt);

    /**
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
            @Param("deadline") AuctionStatus deadline,
            @Param("ended") AuctionStatus ended,
            @Param("now") LocalDateTime now
    );

    /**
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
                    "and (a.status = 'RUNNING' " +
                    "or a.status = 'DEADLINE')")
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

    @Query("""
select a.id from Auction a
where a.status = :status
  and a.endAt <= :now
""")
    List<Long> findIdsToEnd(@Param("status") AuctionStatus status, @Param("now") LocalDateTime now);


    /**
     */
    @EntityGraph(attributePaths = {"item", "item.seller", "item.category", "currentBidder"})
    List<Auction> findByCurrentBidder_IdAndStatusIn(Long userId, List<AuctionStatus> statuses);

    @Query("SELECT a FROM Auction a WHERE a.item.id = :itemId")
    Optional<Auction> findByItemId(@Param("itemId") Long itemId);

    @Query("SELECT a FROM Auction a WHERE a.item.id IN :itemIds")
    List<Auction> findAllByItemIdIn(@Param("itemIds") List<Long> itemIds);
    @EntityGraph(attributePaths = {"item", "item.seller", "item.category"})
    @Query("""
select a from Auction a
where a.status in :statuses
  and (
       a.createdAt >= :threshold
       or (
            a.bidCount = 0
            and not exists (
                select 1
                from Wish w
                where w.item = a.item
            )
       )
  )
""")
    List<Auction> findNewAuctions(@Param("statuses") List<AuctionStatus> statuses,
                                  @Param("threshold") LocalDateTime threshold,
                                  Pageable pageable);
}
