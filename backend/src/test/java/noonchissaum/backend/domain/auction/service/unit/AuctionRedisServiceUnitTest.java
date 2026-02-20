package noonchissaum.backend.domain.auction.service.unit;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.service.AuctionRedisService;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuctionRedisServiceUnitTest {

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("경매 Redis 저장 시 현재가/입찰자/상태 등 스냅샷 키 저장")
    void setRedis_setsSnapshotKeys() {
        AuctionRedisService service = new AuctionRedisService(auctionRepository, redisTemplate);
        Auction auction = sampleAuction(31L);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.RUNNING);
        ReflectionTestUtils.setField(auction, "bidCount", 2);
        ReflectionTestUtils.setField(auction, "currentPrice", BigDecimal.valueOf(13000));
        ReflectionTestUtils.setField(auction, "imminentMinutes", 5);
        ReflectionTestUtils.setField(auction, "isExtended", false);
        when(auctionRepository.findById(31L)).thenReturn(Optional.of(auction));
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);

        service.setRedis(31L);

        verify(ops).set(eq("auction:31:currentPrice"), eq("13000"), any(Duration.class));
        verify(ops).set(eq("auction:31:currentBidCount"), eq("2"), any(Duration.class));
        verify(ops).set(eq("auction:31:status"), eq("RUNNING"), any(Duration.class));
    }

    @Test
    @DisplayName("경매 Redis 저장 시 경매 미존재이면 NOT_FOUND_AUCTIONS 예외 던짐")
    void setRedis_whenAuctionMissing_throwsApiException() {
        AuctionRedisService service = new AuctionRedisService(auctionRepository, redisTemplate);
        when(auctionRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.setRedis(999L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_AUCTIONS);
    }

    @Test
    @DisplayName("경매 취소 시 Redis 경매 키 일괄 삭제")
    void cancelAuction_deletesAuctionKeys() {
        AuctionRedisService service = new AuctionRedisService(auctionRepository, redisTemplate);

        service.cancelAuction(45L);

        verify(redisTemplate).delete(eq(java.util.List.of(
                "auction:45:currentPrice",
                "auction:45:currentBidder",
                "auction:45:currentBidCount",
                "auction:45:status",
                "auction:45:endTime",
                "auction:45:imminentMinutes",
                "auction:45:isExtended"
        )));
    }

    private Auction sampleAuction(Long auctionId) {
        User seller = User.builder()
                .email("auction-redis-unit-seller@test.com")
                .nickname("auction_redis_seller")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(seller, "id", 10L);

        Category category = new Category("auction-redis", null);
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title("auction-redis-item")
                .description("desc")
                .build();

        Auction auction = Auction.builder()
                .item(item)
                .startPrice(BigDecimal.valueOf(10000))
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(auction, "id", auctionId);
        return auction;
    }
}

