package noonchissaum.backend.domain.auction.repository;

import noonchissaum.backend.domain.auction.dto.MyBidAuctionDto;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;


import javax.swing.*;

public interface BidRepository extends JpaRepository<Bid,Long> {
    boolean existsByRequestId(String requestId);

    Page<Bid> findByAuctionIdOrderByCreatedAtDesc(Long auctionId, Pageable pageable);



    // 내가 한 번이라도 입찰한 경매 목록
    @Query("""
        select distinct a
        from Auction a
        join Bid b on b.auction = a
        where b.bidder.id = :userId
        order by a.endAt desc
    """)
    Page<Auction> findParticipatedAuctions(@Param("userId") Long userId, Pageable pageable);

    // 내가 해당 경매에 입찰한 최고가
    @Query("""
        select coalesce(max(b.bidPrice), 0)
        from Bid b
        where b.bidder.id = :userId
          and b.auction.id = :auctionId
    """)
    BigDecimal myMaxBid(@Param("userId") Long userId, @Param("auctionId") Long auctionId);

    // 해당 경매 현재 최고가
    @Query("""
        select coalesce(max(b.bidPrice), 0)
        from Bid b
        where b.auction.id = :auctionId
    """)
    BigDecimal currentMaxBid(@Param("auctionId") Long auctionId);

    // 입찰 횟수
    int countByAuctionId(Long auctionId);
}


