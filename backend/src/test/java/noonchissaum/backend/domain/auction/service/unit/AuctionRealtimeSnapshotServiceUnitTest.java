package noonchissaum.backend.domain.auction.service.unit;

import noonchissaum.backend.domain.auction.dto.ws.AuctionSnapshotPayload;
import noonchissaum.backend.domain.auction.service.AuctionRealtimeSnapshotService;
import noonchissaum.backend.domain.auction.service.AuctionRedisService;
import noonchissaum.backend.global.RedisKeys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuctionRealtimeSnapshotServiceUnitTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private AuctionRedisService auctionRedisService;

    @Test
    @DisplayName("스냅샷 키가 일부 없으면 복구 호출 후 값을 파싱해 반환")
    void getSnapshot_whenKeyMissing_recoversAndParsesValues() {
        AuctionRealtimeSnapshotService service = new AuctionRealtimeSnapshotService(redisTemplate, auctionRedisService);
        long auctionId = 5L;
        long epochMillis = 1768886400000L;

        when(redisTemplate.hasKey(RedisKeys.auctionCurrentPrice(auctionId))).thenReturn(false);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(RedisKeys.auctionCurrentPrice(auctionId))).thenReturn("12345");
        when(ops.get(RedisKeys.auctionCurrentBidder(auctionId))).thenReturn("77");
        when(ops.get(RedisKeys.auctionCurrentBidCount(auctionId))).thenReturn("8");
        when(ops.get(RedisKeys.auctionEndTime(auctionId))).thenReturn(String.valueOf(epochMillis));
        when(ops.get(RedisKeys.auctionImminentMinutes(auctionId))).thenReturn("6");
        when(ops.get(RedisKeys.auctionIsExtended(auctionId))).thenReturn("1");

        AuctionSnapshotPayload snapshot = service.getSnapshot(auctionId);

        verify(auctionRedisService).setRedis(auctionId);
        assertThat(snapshot.getCurrentPrice()).isEqualTo(12345L);
        assertThat(snapshot.getCurrentBidderId()).isEqualTo(77L);
        assertThat(snapshot.getBidCount()).isEqualTo(8);
        assertThat(snapshot.getEndAt()).isEqualTo(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.of("Asia/Seoul")));
        assertThat(snapshot.getImminentMinutes()).isEqualTo(6);
        assertThat(snapshot.getIsExtended()).isTrue();
    }

    @Test
    @DisplayName("getSnapshotIfPresent에서 키 누락이면 empty 반환")
    void getSnapshotIfPresent_whenMissingKey_returnsEmpty() {
        AuctionRealtimeSnapshotService service = new AuctionRealtimeSnapshotService(redisTemplate, auctionRedisService);
        long auctionId = 11L;

        when(redisTemplate.hasKey(RedisKeys.auctionCurrentPrice(auctionId))).thenReturn(false);

        Optional<AuctionSnapshotPayload> result = service.getSnapshotIfPresent(auctionId);

        assertThat(result).isEmpty();
        verify(auctionRedisService, never()).setRedis(auctionId);
    }
}
