package noonchissaum.backend.domain.auction.service;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.entity.AuctionSortType;
import noonchissaum.backend.domain.auction.redis.RedisIndexKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuctionPriceIndexReadService {

    private final StringRedisTemplate redisTemplate;

    public List<Long> getAuctionIdsByCategoryPrice(Long categoryId, AuctionSortType sort, int page, int size) {
        long start = (long) page * size;
        long end = start + size - 1;

        String key = RedisIndexKeys.priceLiveByCategory(categoryId);

        Set<String> raw = (sort == AuctionSortType.PRICE_HIGH)
                ? redisTemplate.opsForZSet().reverseRange(key, start, end)
                : redisTemplate.opsForZSet().range(key, start, end);

        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream().map(Long::valueOf).toList();
    }

    public long countByCategory(Long categoryId) {
        Long cnt = redisTemplate.opsForZSet().zCard(RedisIndexKeys.priceLiveByCategory(categoryId));
        return cnt == null ? 0L : cnt;
    }
}