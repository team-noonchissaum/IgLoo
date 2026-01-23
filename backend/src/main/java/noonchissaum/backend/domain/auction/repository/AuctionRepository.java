package noonchissaum.backend.domain.auction.repository;

import noonchissaum.backend.domain.auction.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
}
