package noonchissaum.backend.domain.auction.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.auction.service.BidRecordService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingBidScheduler {

    private final StringRedisTemplate redisTemplate;
    private final BidRepository bidRepository;
    private final BidRecordService bidRecordService;
    private final WalletRepository walletRepository;
    private final AuctionService auctionService;
    private final UserService userService;

    @Scheduled(fixedDelay = 5 * 60 * 1000) // 5분마다
    public void reconcilePendingBids() {
        Set<String> requestIds = redisTemplate.opsForSet().members("pending_bid_requests");
        if (requestIds == null || requestIds.isEmpty()) return;
        for (String requestId : requestIds) {
            String infoKey = "pending_bid_info:" + requestId;

            Map<Object, Object> info = redisTemplate.opsForHash().entries(infoKey);

            // 이미 DB에 있으면 pending 삭제
            if (bidRepository.existsByRequestId(requestId)) {
                redisTemplate.opsForSet().remove("pending_bid_requests", requestId);
                redisTemplate.delete(infoKey);
                continue;
            }

            try {
                Long auctionId = Long.parseLong((String) info.get("auctionId"));
                Long userId = Long.parseLong((String) info.get("userId"));
                BigDecimal bidAmount = new BigDecimal((String) info.get("bidAmount"));

                String rawPrev = (String) info.get("previousBidderId");
                Long previousBidderId = (rawPrev == null || rawPrev.isBlank()) ? -1L : Long.parseLong(rawPrev);

                String rawRefund = (String) info.get("refundAmount");
                BigDecimal refundAmount = (rawRefund == null || rawRefund.isBlank()) ? BigDecimal.ZERO : new BigDecimal(rawRefund);

                Auction auction = auctionService.getAuction(auctionId);
                User user = userService.getUser(userId);

                // Bid 저장
                if (!bidRepository.existsByRequestId(requestId)) {
                    bidRecordService.saveBidRecord(auction, user, bidAmount, requestId);
                }

                // wallet 저장
                Wallet newWallet = walletRepository.findByUserId(userId)
                        .orElseThrow(() -> new RuntimeException("신규 입찰자 지갑 없음"));
                newWallet.bid(bidAmount);

                if (previousBidderId != null && previousBidderId != -1L) {
                    Wallet prevWallet = walletRepository.findByUserId(previousBidderId)
                            .orElseThrow(() -> new RuntimeException("이전 입찰자 지갑 없음"));
                    prevWallet.bidCanceled(refundAmount);
                }
            }catch (Exception e) {
                log.error("Pending bid 저장 실패. requestId={}, reason={}", requestId, e.toString());
                throw new RuntimeException(e.getMessage());
            }
        }
    }
}