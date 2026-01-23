package noonchissaum.backend.domain.auction.repository;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    @Query("SELECT a FROM Auction a JOIN FETCH a.item i JOIN FETCH i.seller s WHERE a.status = :status")
    Page<Auction> findAllByStatus(@Param("status") AuctionStatus status, Pageable pageable);

    @Query("SELECT a FROM Auction a JOIN FETCH a.item i JOIN FETCH i.seller s")
    Page<Auction> findAllWithItemAndSeller(Pageable pageable);
}

