package noonchissaum.backend.domain.auction.repository;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    @EntityGraph(attributePaths = {"item", "item.seller", "item.category"})
    Page<Auction> findAllByStatus(AuctionStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"item", "item.seller", "item.category"})
    Page<Auction> findAllWithItemAndSeller(Pageable pageable);
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
     * RUNNING -> ENDED
     */
    @Modifying
    @Query("""
    update Auction a
    set a.status = :ended
    where a.status = :running
      and a.endAt <= :now
""")
    int endRunningAuctions(
            AuctionStatus running,
            AuctionStatus ended,
            LocalDateTime now
    );

}

