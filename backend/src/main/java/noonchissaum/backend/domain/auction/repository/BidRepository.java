package noonchissaum.backend.domain.auction.repository;

import noonchissaum.backend.domain.auction.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid,Long> {
    boolean existsByRequestId(String requestId);

}
