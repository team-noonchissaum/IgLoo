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

    @Transactional(readOnly = true)
    public void setRedis(Long auctionId) {
        Auction auction = auctionRepository.findByIdWithStatus(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));

        Duration redisTTL = Duration.between(LocalDateTime.now(), auction.getEndAt().plusMinutes(10));

        redisTemplate.opsForValue().set(RedisKeys.auctionCurrentPrice(auction.getId()), auction.getCurrentPrice().toString(), redisTTL);
        redisTemplate.opsForValue().set(RedisKeys.auctionCurrentBidder(auction.getId()), auction.getCurrentBidder().toString(), redisTTL);
        redisTemplate.opsForValue().set(RedisKeys.auctionCurrentBidCount(auction.getId()), auction.getBidCount().toString(), redisTTL);
        redisTemplate.opsForValue().set(RedisKeys.auctionEndTime(auction.getId()), auction.getEndAt().toString(), redisTTL);
        redisTemplate.opsForValue().set(RedisKeys.auctionImminentMinutes(auction.getId()), auction.getImminentMinutes().toString(), redisTTL);
        redisTemplate.opsForValue().set(RedisKeys.auctionIsExtended(auction.getId()), auction.getIsExtended().toString(), redisTTL);
    }


    @Transactional(readOnly = true)
    public void cancelAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));

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
