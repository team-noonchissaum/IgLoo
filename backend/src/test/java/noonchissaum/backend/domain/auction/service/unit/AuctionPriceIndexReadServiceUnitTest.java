package noonchissaum.backend.domain.auction.service.unit;

import noonchissaum.backend.domain.auction.entity.AuctionSortType;
import noonchissaum.backend.domain.auction.redis.RedisIndexKeys;
import noonchissaum.backend.domain.auction.service.AuctionPriceIndexReadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuctionPriceIndexReadServiceUnitTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("PRICE_HIGH 정렬은 reverseRange를 사용해 ID 목록 변환")
    void getAuctionIdsByCategoryPrice_highSort_usesReverseRange() {
        AuctionPriceIndexReadService service = new AuctionPriceIndexReadService(redisTemplate);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zSetOps = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        Set<String> raw = new LinkedHashSet<>(List.of("5", "3"));
        when(zSetOps.reverseRange(RedisIndexKeys.priceLiveByCategory(2L), 0L, 1L)).thenReturn(raw);

        List<Long> result = service.getAuctionIdsByCategoryPrice(2L, AuctionSortType.PRICE_HIGH, 0, 2);

        assertThat(result).containsExactly(5L, 3L);
        verify(zSetOps).reverseRange(RedisIndexKeys.priceLiveByCategory(2L), 0L, 1L);
    }

    @Test
    @DisplayName("Redis 결과가 비어 있으면 빈 리스트 반환")
    void getAuctionIdsByCategoryPrice_whenEmpty_returnsEmptyList() {
        AuctionPriceIndexReadService service = new AuctionPriceIndexReadService(redisTemplate);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zSetOps = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.range(RedisIndexKeys.priceLiveByCategory(7L), 0L, 9L)).thenReturn(null);

        List<Long> result = service.getAuctionIdsByCategoryPrice(7L, AuctionSortType.PRICE_LOW, 0, 10);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("카테고리 개수 조회가 null이면 0 반환")
    void countByCategory_whenNullCount_returnsZero() {
        AuctionPriceIndexReadService service = new AuctionPriceIndexReadService(redisTemplate);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zSetOps = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard(RedisIndexKeys.priceLiveByCategory(1L))).thenReturn(null);

        long count = service.countByCategory(1L);

        assertThat(count).isZero();
    }
}
