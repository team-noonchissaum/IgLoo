package noonchissaum.backend.domain.auction.service.unit;

import noonchissaum.backend.domain.auction.dto.res.MyBidAuctionRes;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.auction.service.AuctionMessageService;
import noonchissaum.backend.domain.auction.service.AuctionRecordService;
import noonchissaum.backend.domain.auction.service.AuctionRedisService;
import noonchissaum.backend.domain.auction.service.BidService;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.notification.service.NotificationService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.util.UserLockExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BidServiceUnitTest {

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private WalletService walletService;
    @Mock
    private BidRepository bidRepository;
    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private AuctionRedisService auctionRedisService;
    @Mock
    private AuctionMessageService auctionMessageService;
    @Mock
    private AuctionRecordService auctionRecordService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserLockExecutor userLockExecutor;

    @Test
    @DisplayName("중복 requestId 감지 시 DUPLICATE_BID_REQUEST 예외 던짐")
    void placeBid_duplicateRequest_throwsApiException() {
        BidService bidService = new BidService(
                redissonClient, redisTemplate, walletService, bidRepository, auctionRepository,
                eventPublisher, auctionRedisService, auctionMessageService, auctionRecordService,
                notificationService, userLockExecutor
        );

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(any(), eq("Y"), any())).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> bidService.placeBid(1L, 10L, BigDecimal.valueOf(10000), "req-dup"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_BID_REQUEST);
        verify(redissonClient, never()).getLock(anyString());
    }

    @Test
    @DisplayName("내 입찰 경매 목록 매핑 및 최고가/최고입찰자 계산 위임")
    void getMyBidAuctions_mapsValues_andDelegatesRepositoryCalls() {
        BidService bidService = new BidService(
                redissonClient, redisTemplate, walletService, bidRepository, auctionRepository,
                eventPublisher, auctionRedisService, auctionMessageService, auctionRecordService,
                notificationService, userLockExecutor
        );

        User seller = User.builder()
                .email("seller@test.com")
                .nickname("seller")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        Category category = new Category("cat-unit", null);
        Item item1 = new Item(seller, category, "item-1", "desc", BigDecimal.valueOf(1000));
        Item item2 = new Item(seller, category, "item-2", "desc", BigDecimal.valueOf(1000));

        Auction a1 = Auction.builder()
                .item(item1)
                .startPrice(BigDecimal.valueOf(1000))
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusHours(1))
                .build();
        Auction a2 = Auction.builder()
                .item(item2)
                .startPrice(BigDecimal.valueOf(1000))
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusHours(2))
                .build();
        a1.run();
        a2.run();
        ReflectionTestUtils.setField(a1, "id", 101L);
        ReflectionTestUtils.setField(a2, "id", 202L);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Auction> auctions = new PageImpl<>(List.of(a1, a2), pageable, 2);
        when(bidRepository.findParticipatedAuctions(55L, pageable)).thenReturn(auctions);

        when(bidRepository.myMaxBid(55L, 101L)).thenReturn(BigDecimal.valueOf(1500));
        when(bidRepository.currentMaxBid(101L)).thenReturn(BigDecimal.valueOf(3000));
        when(bidRepository.countByAuctionId(101L)).thenReturn(3);

        when(bidRepository.myMaxBid(55L, 202L)).thenReturn(BigDecimal.valueOf(2000));
        when(bidRepository.currentMaxBid(202L)).thenReturn(BigDecimal.valueOf(2000));
        when(bidRepository.countByAuctionId(202L)).thenReturn(1);

        Page<MyBidAuctionRes> result = bidService.getMyBidAuctions(55L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);

        MyBidAuctionRes first = result.getContent().get(0);
        assertThat(first.auctionId()).isEqualTo(101L);
        assertThat(first.itemTitle()).isEqualTo("item-1");
        assertThat(first.myHighestBidPrice()).isEqualTo(1500L);
        assertThat(first.currentPrice()).isEqualTo(3000L);
        assertThat(first.isHighestBidder()).isFalse();
        assertThat(first.auctionStatus()).isEqualTo(AuctionStatus.RUNNING);
        assertThat(first.bidCount()).isEqualTo(3);

        MyBidAuctionRes second = result.getContent().get(1);
        assertThat(second.auctionId()).isEqualTo(202L);
        assertThat(second.itemTitle()).isEqualTo("item-2");
        assertThat(second.myHighestBidPrice()).isEqualTo(2000L);
        assertThat(second.currentPrice()).isEqualTo(2000L);
        assertThat(second.isHighestBidder()).isTrue();
        assertThat(second.auctionStatus()).isEqualTo(AuctionStatus.RUNNING);
        assertThat(second.bidCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("음수 페이지 요청 시 INVALID_PAGE_REQUEST 예외 던짐")
    void getMyBidAuctions_invalidPageable_throwsIllegalArgument() {
        BidService bidService = new BidService(
                redissonClient, redisTemplate, walletService, bidRepository, auctionRepository,
                eventPublisher, auctionRedisService, auctionMessageService, auctionRecordService,
                notificationService, userLockExecutor
        );

        Pageable invalidPageable = mock(Pageable.class);
        when(invalidPageable.getPageNumber()).thenReturn(-1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bidService.getMyBidAuctions(1L, invalidPageable));

        assertThat(ex.getMessage()).isEqualTo("INVALID_PAGE_REQUEST");
    }

    @Test
    @DisplayName("요청 ID 존재 여부 조회 위임")
    void isExistRequestId_delegatesRepository() {
        BidService bidService = new BidService(
                redissonClient, redisTemplate, walletService, bidRepository, auctionRepository,
                eventPublisher, auctionRedisService, auctionMessageService, auctionRecordService,
                notificationService, userLockExecutor
        );

        when(bidRepository.existsByRequestId("req-1")).thenReturn(true);

        boolean exists = bidService.isExistRequestId("req-1");

        assertThat(exists).isTrue();
        verify(bidRepository).existsByRequestId("req-1");
    }
}
