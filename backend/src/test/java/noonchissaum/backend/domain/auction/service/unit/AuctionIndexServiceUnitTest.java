package noonchissaum.backend.domain.auction.service.unit;

import noonchissaum.backend.domain.auction.redis.RedisIndexKeys;
import noonchissaum.backend.domain.auction.service.AuctionIndexService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuctionIndexServiceUnitTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("가격 인덱스 갱신 시 전체/카테고리 ZSET에 모두 반영")
    void updatePriceIndex_addsToBothZSets() {
        AuctionIndexService service = new AuctionIndexService(redisTemplate);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zSetOps = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);

        service.updatePriceIndex(1L, 2L, BigDecimal.valueOf(12345));

        verify(zSetOps).add(RedisIndexKeys.PRICE_LIVE_ZSET, "1", 12345d);
        verify(zSetOps).add(RedisIndexKeys.priceLiveByCategory(2L), "1", 12345d);
    }

    @Test
    @DisplayName("필수 값이 null이면 인덱스 갱신을 수행하지 않음")
    void updatePriceIndex_withNullInput_skipsUpdate() {
        AuctionIndexService service = new AuctionIndexService(redisTemplate);

        service.updatePriceIndex(null, 2L, BigDecimal.TEN);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("카테고리 카운트 조회에서 null 응답이면 0 반환")
    void countByCategory_whenNullCount_returnsZero() {
        AuctionIndexService service = new AuctionIndexService(redisTemplate);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zSetOps = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(zSetOps.zCard(RedisIndexKeys.priceLiveByCategory(3L))).thenReturn(null);

        long count = service.countByCategory(3L);

        assertThat(count).isZero();
    }
}
