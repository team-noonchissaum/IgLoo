package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.redis.RedisIndexKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuctionIndexService {

    private final StringRedisTemplate redisTemplate;

    public void updatePriceIndex(Long auctionId, Long categoryId, BigDecimal price) {
        if (auctionId == null || categoryId == null || price == null) return;

        double score = price.doubleValue();

        // (옵션) live 전체도 같이 유지
        redisTemplate.opsForZSet().add(RedisIndexKeys.PRICE_LIVE_ZSET, auctionId.toString(), score);

        // 카테고리별
        redisTemplate.opsForZSet().add(RedisIndexKeys.priceLiveByCategory(categoryId), auctionId.toString(), score);
    }

    public void removeFromIndex(Long auctionId, Long categoryId) {
        if (auctionId == null) return;

        redisTemplate.opsForZSet().remove(RedisIndexKeys.PRICE_LIVE_ZSET, auctionId.toString());

        if (categoryId != null) {
            redisTemplate.opsForZSet().remove(RedisIndexKeys.priceLiveByCategory(categoryId), auctionId.toString());
        }
    }

    public long countByCategory(Long categoryId) {
        Long cnt = redisTemplate.opsForZSet().zCard(RedisIndexKeys.priceLiveByCategory(categoryId));
        return cnt == null ? 0L : cnt;
    }
}