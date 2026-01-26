package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidService {

    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;
    private final WalletService walletService;
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final BidRecordService bidRecordService;

    public void placeBid(Long auctionId, Long userId, BigDecimal bidAmount, String requestId) {
        // 1. 멱등성 체크 (락 획득 전 수행하여 불필요한 대기 방지)
        // requestId는 FE가 UUID를 이용해서 담당
        String requestKey = "bid_idempotency:" + requestId;
        if (Boolean.FALSE.equals(redisTemplate.opsForValue().setIfAbsent(requestKey, "Y", Duration.ofMinutes(10)))) {
            throw new ApiException(ErrorCode.DUPLICATE_BID_REQUEST);
        }

        RLock lock = redissonClient.getLock("lock:auction:" + auctionId);

        try{
            boolean available = lock.tryLock(5, 2, TimeUnit.SECONDS);
            // 입찰 조건 확인 로직

            if (!available){
                throw new ApiException(ErrorCode.BID_LOCK_ACQUISITION);
            }
            String priceKey = "auction:" + auctionId + ":currentPrice";
            String bidderKey = "auction:" + auctionId + ":currentBidder";
            String bidCount = "auction:" + auctionId + ":currentBidCount";

            String rawPreviousBidderId = redisTemplate.opsForValue().get(bidderKey);
            Long previousBidderId = rawPreviousBidderId != null ? Long.parseLong(rawPreviousBidderId) : -1L;

            String rawPrice = redisTemplate.opsForValue().get(priceKey);
            BigDecimal currentPrice = rawPrice != null ? new BigDecimal(rawPrice) : BigDecimal.ZERO;

            walletService.getBalance(userId);
            walletService.getBalance(previousBidderId);

            validateBidConditions(auctionId,userId, bidAmount);

            // 이전 비더 유저 돈 환불 + 신규 비더 돈 Lock
            // previousBidderId가 null인 경우에는 신규 입찰자이므로 walletService에서 처리 필요
            walletService.processBidWallet(userId, previousBidderId, bidAmount, currentPrice);


            String rawBidCount  = redisTemplate.opsForValue().get(bidCount);
            String bidCountStr = rawBidCount != null ? rawBidCount : "0";

            Integer bidCountInt = Integer.parseInt(bidCountStr);
            // Redis 새로운 1등 정보 저장
            redisTemplate.opsForValue().set(priceKey, String.valueOf(bidAmount));
            redisTemplate.opsForValue().set(bidderKey, String.valueOf(userId));
            redisTemplate.opsForValue().set(bidCount, String.valueOf(++bidCountInt));

            // Stomp 메세지 발행 로직
            // messageService.sendPriceUpdate(auctionId, bidAmount);

            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_AUCTIONS));
            User user = userService.getUser(userId);

            // 1. Redis에 복구용 전체 정보 저장
            Map<String, String> bidInfo = new HashMap<>();
            bidInfo.put("auctionId", String.valueOf(auctionId));
            bidInfo.put("userId", String.valueOf(userId));
            bidInfo.put("bidAmount", bidAmount.toString());
            bidInfo.put("requestId", requestId);

            String infoKey = "pending_bid_info:" + requestId;
            redisTemplate.opsForHash().putAll(infoKey, bidInfo);
            redisTemplate.expire(infoKey, Duration.ofMinutes(10));

            redisTemplate.opsForSet().add("pending_bid_requests", requestId);

            redisTemplate.opsForValue().set(requestId, bidAmount+"");

            bidRecordService.saveBidRecord(auction, user, bidAmount, requestId);
        }
        catch (InterruptedException e){
            Thread.currentThread().interrupt();
            return ;
        } catch (Exception e) {
            redisTemplate.delete(requestKey);
            throw e;
        } finally {
            // 락 해제
            if (lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }

    /**
     * 입찰 조건 검증 로직:
     * 1. 현재 최고 입찰가보다 10%(10원 단위 올림) 이상 높은 금액인지 체크.
     * 2. 본인이 현재 최고 입찰자인지 확인 (연속 입찰 제한 정책).
     * 3. 해당 경매의 상태가 RUNNING이며, 종료 시간이 지나지 않았는지 확인.
     * 4. 잔액 사전 검증:입찰 시도 금액만큼 사용자의 가용 잔액(balance)이 충분한지 지갑 서비스와 연동하여 확인
     */
    private void validateBidConditions(
            Long auctionId,
            Long userId,
            BigDecimal bidAmount
    ) {
        String priceKey = "auction:" + auctionId + ":currentPrice";
        String bidderKey = "auction:" + auctionId + ":currentBidder";
        String userBalance = "user:" + userId + ":balance";

        String rawPrice = redisTemplate.opsForValue().get(priceKey);
        BigDecimal currentPrice = (rawPrice == null || rawPrice.isBlank()) ? BigDecimal.ZERO : new BigDecimal(rawPrice);

        //10% 이상 체크 (10원 단위 올림)
        if (currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal minBid = currentPrice.multiply(new BigDecimal("1.1"))
                    .setScale(-1, RoundingMode.CEILING);

            if (bidAmount.compareTo(minBid) < 0) {
                throw new ApiException(ErrorCode.LOW_BID_AMOUNT);
            }
        }

        //연속 입찰 체크
        String rawBidderId = redisTemplate.opsForValue().get(bidderKey);
        if (rawBidderId != null && !rawBidderId.isBlank()) {
            Long currentBidderId = Long.parseLong(rawBidderId);
            if (currentBidderId.equals(userId)) {
                throw new ApiException(ErrorCode.CANNOT_BID_CONTINUOUS);
            }
        }

        //경매 상태 체크
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("해당 경매는 존재하지 않습니다."));
        if (!auction.getStatus().equals(AuctionStatus.RUNNING)) {
            throw new ApiException(ErrorCode.NOT_FOUND_AUCTIONS);
        }

        //잔액 사전 검증 (가용 잔액)
        String rawUserBalance = redisTemplate.opsForValue().get(userBalance);
        BigDecimal currentUserBalance = BigDecimal.valueOf(Long.parseLong(rawUserBalance));
        if (currentUserBalance.compareTo(bidAmount) < 0) {
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    public Bid getBid(Long bidId) {
        return bidRepository.findById(bidId)
                .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_BID));
    }
}
