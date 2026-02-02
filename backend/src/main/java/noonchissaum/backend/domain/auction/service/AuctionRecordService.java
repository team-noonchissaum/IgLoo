package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionRecordService {

    private final AuctionRepository auctionRepository;
    private final UserService userService;
    private final AuctionRedisService auctionRedisService;
    private final AuctionIndexService auctionIndexService;

    @Transactional
    public void saveAuction(Long auctionId, Long userId, BigDecimal bidAmount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("auction not found"));
        User user = userService.getUserByUserId(userId);

        auction.updateBid(user, bidAmount);
        // Redis 스냅샷 갱신
        auctionRedisService.setRedis(auctionId);

        // Redis ZSET 가격 인덱스 갱신 (카테고리+가격정렬 )
        Long categoryId = auction.getItem().getCategory().getId();
        auctionIndexService.updatePriceIndex(auctionId, categoryId, auction.getCurrentPrice());
    }
}
