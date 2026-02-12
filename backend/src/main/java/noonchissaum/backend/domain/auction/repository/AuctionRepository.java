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

    // Redis id 由ъ뒪???곸꽭 濡쒕뵫??
    @EntityGraph(attributePaths = {"item", "item.seller", "item.category"})
    List<Auction> findByIdIn(List<Long> ids);

    /**
     *?ㅼ?以?愿???곹깭媛?蹂寃쎌옘由?
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
     *?ㅼ?以?愿???곹깭媛?蹂寃쎌옘由?
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
     * ?ㅼ?以?愿???곹깭媛?蹂寃?荑쇰━
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

    //deadline?쇰줈 諛붾?寃쎈ℓ 李얘린
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

    @Query("""
select a.id from Auction a
where a.status = :status
  and a.endAt <= :now
""")
    List<Long> findIdsToEnd(@Param("status") AuctionStatus status, @Param("now") LocalDateTime now);


    /**
     * 李⑤떒 ?좎?媛 理쒖긽???낆같?먯씤 吏꾪뻾 以?寃쎈ℓ 議고쉶 (濡ㅻ갚 ???
     */
    @EntityGraph(attributePaths = {"item", "item.seller", "item.category", "currentBidder"})
    List<Auction> findByCurrentBidder_IdAndStatusIn(Long userId, List<AuctionStatus> statuses);

    // Item ID濡?寃쎈ℓ瑜?李얠뒿?덈떎.
    @Query("SELECT a FROM Auction a WHERE a.item.id = :itemId")
    Optional<Auction> findByItemId(@Param("itemId") Long itemId);

    // ?щ윭 Item ID濡?寃쎈ℓ 紐⑸줉??李얠뒿?덈떎.
    @Query("SELECT a FROM Auction a WHERE a.item.id IN :itemIds")
    List<Auction> findAllByItemIdIn(@Param("itemIds") List<Long> itemIds);
    @EntityGraph(attributePaths = {"item", "item.seller", "item.category"})
    @Query("""
select a from Auction a
where a.status in :statuses
  and (a.bidCount = 0 or a.createdAt >= :threshold)
""")
    List<Auction> findNewAuctions(@Param("statuses") List<AuctionStatus> statuses,
                                  @Param("threshold") LocalDateTime threshold,
                                  Pageable pageable);
}
