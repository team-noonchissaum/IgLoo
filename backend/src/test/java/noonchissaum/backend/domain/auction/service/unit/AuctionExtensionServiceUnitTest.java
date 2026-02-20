package noonchissaum.backend.domain.auction.service.unit;

import noonchissaum.backend.domain.auction.dto.ws.AuctionExtendedPayload;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.service.AuctionExtensionService;
import noonchissaum.backend.domain.auction.service.AuctionMessageService;
import noonchissaum.backend.global.RedisKeys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuctionExtensionServiceUnitTest {

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private AuctionMessageService auctionMessageService;

    @Test
    @DisplayName("연장 조건 충족 시 Redis 동기화와 연장 메시지 전송")
    void extension_whenExtended_updatesRedisAndSendsMessage() {
        AuctionExtensionService service = new AuctionExtensionService(auctionRepository, redisTemplate, auctionMessageService);
        Auction auction = Auction.builder()
                .item(null)
                .startPrice(BigDecimal.valueOf(1000))
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusMinutes(1))
                .build();
        ReflectionTestUtils.setField(auction, "id", 10L);

        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);

        boolean result = service.extension(10L);

        assertThat(result).isTrue();
        verify(ops).set(RedisKeys.auctionEndTime(10L), auction.getEndAt().toString());
        verify(ops).set(RedisKeys.auctionIsExtended(10L), "true");

        ArgumentCaptor<AuctionExtendedPayload> payloadCaptor = ArgumentCaptor.forClass(AuctionExtendedPayload.class);
        verify(auctionMessageService).sendAuctionExtended(org.mockito.ArgumentMatchers.eq(10L), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue().getAuctionId()).isEqualTo(10L);
        assertThat(payloadCaptor.getValue().isExtended()).isTrue();
    }

    @Test
    @DisplayName("연장 조건 불충족 시 false 반환하고 메시지를 보내지 않음")
    void extension_whenNotExtended_returnsFalse() {
        AuctionExtensionService service = new AuctionExtensionService(auctionRepository, redisTemplate, auctionMessageService);
        Auction auction = Auction.builder()
                .item(null)
                .startPrice(BigDecimal.valueOf(1000))
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(auction, "id", 11L);

        when(auctionRepository.findById(11L)).thenReturn(Optional.of(auction));

        boolean result = service.extension(11L);

        assertThat(result).isFalse();
        verify(auctionMessageService, never()).sendAuctionExtended(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }
}
