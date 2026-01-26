package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.wallet.service.BidRecordService;
import noonchissaum.backend.domain.wallet.service.WalletService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    public void placeBid(Long auctionId, Long userId, BigDecimal bidAmount) {
        RLock lock = redissonClient.getLock("lock:auction:" + auctionId);
        // 입찰 조건 확인 로직


        try{
            boolean available = lock.tryLock(5, 2, TimeUnit.SECONDS);

            if (!available){
                throw new RuntimeException("입찰자가 많아 처리에 실패했습니다. 다시 시도해주세요");
            }
            String priceKey = "auction:" + auctionId + ":currentPrice";
            String bidderKey = "auction:" + auctionId + ":currentBidder";
            String bidCount = "auction:" + auctionId + ":currentBidCount";


            // 현재 경매가 조회 및 검증
            String rawPrice = redisTemplate.opsForValue().get(priceKey);
            String priceNum = rawPrice != null ? rawPrice : "0";
            BigDecimal currentPrice = new BigDecimal(priceNum);

            if (bidAmount.compareTo(currentPrice) <= 0) {
                throw new RuntimeException("높은 가격으로 입찰해야 합니다.");
            }

            String rawPreviousBidderId = redisTemplate.opsForValue().get(bidderKey);
            Long previousBidderId = rawPreviousBidderId != null ? Long.parseLong(rawPreviousBidderId) : -1L;

            // 이전 비더 유저 돈 환불 + 신규 비더 돈 Lock
            // previousBidderId가 null인 경우에는 신규 입찰자이므로 walletService에서 처리 필요
            walletService.processBidWallet(userId, previousBidderId, bidAmount, currentPrice);


            String rawBidCount  = redisTemplate.opsForValue().get(bidCount);
            String bidCountStr = rawBidCount != null ? rawBidCount : "0";

            Integer bidCountInt = Integer.parseInt(bidCountStr);
            // Redis 새로운 1등 정보 저장
            redisTemplate.opsForValue().set(priceKey, String.valueOf(bidAmount));
            redisTemplate.opsForValue().set(bidderKey, String.valueOf(userId));
            redisTemplate.opsForValue().set(bidCount, String.valueOf(bidCountInt++));

            // Stomp 메세지 발행 로직
            // messageService.sendPriceUpdate(auctionId, bidAmount);

            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new RuntimeException("해당되는 경매가 없습니다."));
            User user = userService.getUser(userId);

            bidRecordService.saveBidRecord(auction, user, bidAmount);
        }
        catch (InterruptedException e){
            Thread.currentThread().interrupt();
            return ;
        } finally {
            // 락 해제
            if (lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }

    public Bid getBid(Long bidId) {
        return bidRepository.findById(bidId)
                .orElseThrow(() -> new RuntimeException(""));
    }
}
