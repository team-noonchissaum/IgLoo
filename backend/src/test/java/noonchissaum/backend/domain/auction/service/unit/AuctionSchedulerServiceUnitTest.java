package noonchissaum.backend.domain.auction.service.unit;

import noonchissaum.backend.domain.auction.dto.ws.AuctionSnapshotPayload;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.auction.service.AuctionMessageService;
import noonchissaum.backend.domain.auction.service.AuctionRealtimeSnapshotService;
import noonchissaum.backend.domain.auction.service.AuctionRedisService;
import noonchissaum.backend.domain.auction.service.AuctionSchedulerService;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.notification.service.AuctionNotificationService;
import noonchissaum.backend.domain.order.service.OrderService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.CategorySubscriptionRepository;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.service.MailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuctionSchedulerServiceUnitTest {

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private BidRepository bidRepository;
    @Mock
    private OrderService orderService;
    @Mock
    private AuctionRealtimeSnapshotService snapshotService;
    @Mock
    private AuctionMessageService auctionMessageService;
    @Mock
    private AuctionNotificationService auctionNotificationService;
    @Mock
    private AuctionRedisService auctionRedisService;
    @Mock
    private WalletService walletService;
    @Mock
    private CategorySubscriptionRepository categorySubscriptionRepository;
    @Mock
    private MailService mailService;

    @Test
    @DisplayName("경매 노출 처리 시 READY 경매를 RUNNING으로 전환하고 환불/Redis 반영")
    void expose_updatesAuctionsAndReturnsUpdatedCount() {
        AuctionSchedulerService service = new AuctionSchedulerService(
                auctionRepository, bidRepository, orderService, snapshotService, auctionMessageService,
                auctionNotificationService, auctionRedisService, walletService, categorySubscriptionRepository, mailService
        );
        Auction a1 = sampleReadyAuction(101L, 11L, "scheduler-1");
        Auction a2 = sampleReadyAuction(102L, 12L, "scheduler-2");
        when(auctionRepository.findReadyNormalAuctions(any(), any())).thenReturn(Optional.of(List.of(a1, a2)));
        when(categorySubscriptionRepository.findActiveUserEmailsByCategoryId(any())).thenReturn(List.of());

        int updated = service.expose(LocalDateTime.now());

        assertThat(updated).isEqualTo(2);
        assertThat(a1.getStatus()).isEqualTo(AuctionStatus.RUNNING);
        assertThat(a2.getStatus()).isEqualTo(AuctionStatus.RUNNING);
        verify(walletService).setAuctionDeposit(11L, 101L, 1000, "refund");
        verify(walletService).setAuctionDeposit(12L, 102L, 1000, "refund");
        verify(auctionRedisService).setRedis(101L);
        verify(auctionRedisService).setRedis(102L);
    }

    @Test
    @DisplayName("데드라인 마킹 시 대상 경매가 없으면 후속 알림 처리 생략")
    void markDeadline_whenNoTargetIds_skipsFollowUp() {
        AuctionSchedulerService service = new AuctionSchedulerService(
                auctionRepository, bidRepository, orderService, snapshotService, auctionMessageService,
                auctionNotificationService, auctionRedisService, walletService, categorySubscriptionRepository, mailService
        );
        when(auctionRepository.findRunningAuctionsToDeadline(any())).thenReturn(List.of());

        service.markDeadline();

        verify(auctionRepository, never()).markDeadlineAuctions(any());
        verify(auctionNotificationService, never()).notifyImminent(any());
    }

    @Test
    @DisplayName("경매 종료 처리 시 종료 이벤트를 브로드캐스트하고 업데이트 건수를 반환")
    void end_sendsAuctionEndedEventsAndReturnsUpdatedCount() {
        AuctionSchedulerService service = new AuctionSchedulerService(
                auctionRepository, bidRepository, orderService, snapshotService, auctionMessageService,
                auctionNotificationService, auctionRedisService, walletService, categorySubscriptionRepository, mailService
        );
        when(auctionRepository.findIdsToEnd(AuctionStatus.DEADLINE, LocalDateTime.of(2026, 1, 1, 0, 0)))
                .thenReturn(List.of(501L));
        when(auctionRepository.endRunningAuctions(AuctionStatus.DEADLINE, AuctionStatus.ENDED, LocalDateTime.of(2026, 1, 1, 0, 0)))
                .thenReturn(1);
        when(snapshotService.getSnapshot(501L))
                .thenReturn(new AuctionSnapshotPayload(501L, 35000L, 77L, 3, LocalDateTime.now(), 5, false));

        int updated = service.end(LocalDateTime.of(2026, 1, 1, 0, 0));

        assertThat(updated).isEqualTo(1);
        verify(auctionRedisService).setRedis(501L);
        verify(auctionMessageService).sendAuctionEnded(org.mockito.ArgumentMatchers.eq(501L), any());
    }

    private Auction sampleReadyAuction(Long auctionId, Long sellerId, String suffix) {
        User seller = User.builder()
                .email("auction-scheduler-unit-seller-" + suffix + "@test.com")
                .nickname("auction_scheduler_seller_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(seller, "id", sellerId);

        Category category = new Category("category-" + suffix, null);
        ReflectionTestUtils.setField(category, "id", 100L + auctionId);

        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title("title-" + suffix)
                .description("desc")
                .build();

        Auction auction = Auction.builder()
                .item(item)
                .startPrice(BigDecimal.valueOf(10000))
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(auction, "id", auctionId);
        ReflectionTestUtils.setField(auction, "createdAt", LocalDateTime.now().minusMinutes(6));
        return auction;
    }
}

