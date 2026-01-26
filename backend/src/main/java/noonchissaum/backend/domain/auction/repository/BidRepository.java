package noonchissaum.backend.domain.auction.repository;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid,Long> {
    Optional<Bid> findByAuctionAndBidder(Auction auction, User user);
}
