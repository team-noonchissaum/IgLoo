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
            @Param("deadline") AuctionStatus deadline,
            @Param("ended") AuctionStatus ended,
            @Param("now") LocalDateTime now
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

    @Query("""
select a.id from Auction a
where a.status = :status
  and a.endAt <= :now
""")
    List<Long> findIdsToEnd(@Param("status") AuctionStatus status, @Param("now") LocalDateTime now);


    /**
     * 차단 유저가 최상위 입찰자인 진행 중 경매 조회 (롤백 대상)
     */
    @EntityGraph(attributePaths = {"item", "item.seller", "item.category", "currentBidder"})
    List<Auction> findByCurrentBidder_IdAndStatusIn(Long userId, List<AuctionStatus> statuses);


    /**
     * 사용자의 위치 필터링  부분 n+1/lazyloading방지
     */
    @EntityGraph(attributePaths = {"item", "item.seller", "item.category"})
    @Query("""
    SELECT a FROM Auction a
    JOIN FETCH a.item i
    WHERE i.seller.latitude IS NOT NULL
    AND i.seller.longitude IS NOT NULL
    AND i.seller.latitude BETWEEN :minLat AND :maxLat
    AND i.seller.longitude BETWEEN :minLon AND :maxLon
    AND a.status IN ('RUNNING', 'READY','DEADLINE')
    """)
    List<Auction> findAuctionsInBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon
    );
}


