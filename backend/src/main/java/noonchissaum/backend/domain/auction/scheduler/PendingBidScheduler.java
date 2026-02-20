package noonchissaum.backend.domain.auction.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.auction.service.BidRecordService;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
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

    @Scheduled(fixedDelay = 5 * 60 * 1000) // 5분마다
    public void reconcilePendingBids() {

        Set<String> requestIds =
                redisTemplate.opsForSet().members(RedisKeys.pendingBidRequestsSet());

        if (requestIds == null || requestIds.isEmpty()) return;

        for (String requestId : requestIds) {

            String infoKey = RedisKeys.pendingBidInfo(requestId);
            Map<Object, Object> info =
                    redisTemplate.opsForHash().entries(infoKey);

            // 이미 DB에 있으면 pending 삭제
            if (bidRepository.existsByRequestId(requestId)) {
                redisTemplate.opsForSet()
                        .remove(RedisKeys.pendingBidRequestsSet(), requestId);
                redisTemplate.delete(infoKey);
                continue;
            }

            try {
                Long auctionId = Long.parseLong((String) info.get("auctionId"));
                Long userId = Long.parseLong((String) info.get("userId"));
                BigDecimal bidAmount = new BigDecimal((String) info.get("bidAmount"));

                String rawPrev = (String) info.get("previousBidderId");
                Long previousBidderId =
                        (rawPrev == null || rawPrev.isBlank())
                                ? -1L
                                : Long.parseLong(rawPrev);

                String rawRefund = (String) info.get("refundAmount");
                BigDecimal refundAmount =
                        (rawRefund == null || rawRefund.isBlank())
                                ? BigDecimal.ZERO
                                : new BigDecimal(rawRefund);

                // Bid 저장
                if (!bidRepository.existsByRequestId(requestId)) {
                    bidRecordService.saveBidRecord(
                            auctionId,
                            userId,
                            bidAmount,
                            requestId
                    );
                }

                // Wallet 저장
                Wallet newWallet = walletRepository.findByUserId(userId)
                        .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
                newWallet.bid(bidAmount);

                if (previousBidderId != null && previousBidderId != -1L) {
                    Wallet prevWallet = walletRepository.findByUserId(previousBidderId)
                            .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
                    prevWallet.bidCanceled(refundAmount);
                }

            } catch (Exception e) {
                log.error("Pending bid 저장 실패. requestId={}, reason={}", requestId, e.toString());
            }
        }
    }
}
