package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BidRecordService {
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserService userService;

    @Transactional

    //@Async
    public void saveBidRecord(Long auctionId, Long userId, BigDecimal bidAmount, String requestId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("auction not found"));
        User user = userService.getUserByUserId(userId);

        Bid bid = new Bid(auction, user, bidAmount, requestId);
        bidRepository.save(bid);
    }
}
