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

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void reconcilePendingBids() {

        Set<String> requestIds =
                redisTemplate.opsForSet().members(RedisKeys.pendingBidRequestsSet());

        if (requestIds == null || requestIds.isEmpty()) return;

        for (String requestId : requestIds) {

            String infoKey = RedisKeys.pendingBidInfo(requestId);
            Map<Object, Object> info =
                    redisTemplate.opsForHash().entries(infoKey);

            // Already in DB: only cleanup pending markers.
            if (bidRepository.existsByRequestId(requestId)) {
                cleanupPendingKeys(requestId, infoKey, info);
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

                if (!bidRepository.existsByRequestId(requestId)) {
                    bidRecordService.saveBidRecord(
                            auctionId,
                            userId,
                            bidAmount,
                            requestId
                    );
                }

                Wallet newWallet = walletRepository.findByUserId(userId)
                        .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
                newWallet.bid(bidAmount);

                if (previousBidderId != null && previousBidderId != -1L) {
                    Wallet prevWallet = walletRepository.findByUserId(previousBidderId)
                            .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
                    prevWallet.bidCanceled(refundAmount);
                }

                cleanupPendingKeys(requestId, infoKey, info);

            } catch (Exception e) {
                log.error("Pending bid reconciliation failed. requestId={}, reason={}", requestId, e.toString());
                continue;
            }
        }
    }

    private void cleanupPendingKeys(String requestId, String infoKey, Map<Object, Object> info) {
        redisTemplate.opsForSet().remove(RedisKeys.pendingBidRequestsSet(), requestId);

        if (info != null && !info.isEmpty()) {
            Object rawUserId = info.get("userId");
            if (rawUserId instanceof String userId && !userId.isBlank()) {
                redisTemplate.opsForSet().remove(RedisKeys.pendingUser(Long.parseLong(userId)), requestId);
            }

            Object rawPrev = info.get("previousBidderId");
            if (rawPrev instanceof String prevUserId && !prevUserId.isBlank()) {
                Long parsedPrevUserId = Long.parseLong(prevUserId);
                if (parsedPrevUserId != -1L) {
                    redisTemplate.opsForSet().remove(RedisKeys.pendingUser(parsedPrevUserId), requestId);
                }
            }
        }

        redisTemplate.delete(infoKey);
    }
}