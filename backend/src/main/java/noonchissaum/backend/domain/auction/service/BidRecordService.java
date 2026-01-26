package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BidRecordService {
    private final BidRepository bidRepository;

    @Transactional
    public void saveBidRecord(Auction auction, User user, BigDecimal bidAmount , String requestId) {
        Bid bid = new Bid(auction, user, bidAmount , requestId);
        bidRepository.save(bid);
    }
}
