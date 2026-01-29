package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.global.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuctionRedisService {

    private final AuctionRepository auctionRepository;
    private final StringRedisTemplate redisTemplate;

    public void setRedis(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));

        // TTL: endAt + 10분까지 유지, 음수면 최소 1분
        Duration ttl = Duration.between(LocalDateTime.now(), auction.getEndAt().plusMinutes(10));
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofMinutes(1);
        }

        String currentPrice = auction.getCurrentPrice() == null
                ? "0"
                : auction.getCurrentPrice().toPlainString();

        String bidderId = auction.getCurrentBidder() == null
                ? "" // 또는 "0"
                : String.valueOf(auction.getCurrentBidder().getId());

        String bidCount = auction.getBidCount() == null
                ? "0"
                : String.valueOf(auction.getBidCount());

        String endAt = auction.getEndAt() == null
                ? ""
                : auction.getEndAt().toString();

        String imminentMinutes = auction.getImminentMinutes() == null
                ? "0"
                : String.valueOf(auction.getImminentMinutes());

        String isExtended = auction.getIsExtended() == null
                ? "false"
                : String.valueOf(auction.getIsExtended());

        redisTemplate.opsForValue().set(RedisKeys.auctionCurrentPrice(auctionId), currentPrice, ttl);
        redisTemplate.opsForValue().set(RedisKeys.auctionCurrentBidder(auctionId), bidderId, ttl);
        redisTemplate.opsForValue().set(RedisKeys.auctionCurrentBidCount(auctionId), bidCount, ttl);
        redisTemplate.opsForValue().set(RedisKeys.auctionEndTime(auctionId), endAt, ttl);
        redisTemplate.opsForValue().set(RedisKeys.auctionImminentMinutes(auctionId), imminentMinutes, ttl);
        redisTemplate.opsForValue().set(RedisKeys.auctionIsExtended(auctionId), isExtended, ttl);
    }

    public void cancelAuction(Long auctionId) {
        redisTemplate.delete(List.of(
                RedisKeys.auctionCurrentPrice(auctionId),
                RedisKeys.auctionCurrentBidder(auctionId),
                RedisKeys.auctionCurrentBidCount(auctionId),
                RedisKeys.auctionEndTime(auctionId),
                RedisKeys.auctionImminentMinutes(auctionId),
                RedisKeys.auctionIsExtended(auctionId)
        ));
    }
}
