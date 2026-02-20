package noonchissaum.backend.domain.notification.service;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.auction.service.AuctionMessageService;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.notification.constants.NotificationConstants;
import noonchissaum.backend.domain.notification.entity.Notification;
import noonchissaum.backend.domain.notification.entity.NotificationType;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuctionNotificationServiceTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private BidRepository bidRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuctionMessageService auctionMessageService;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuctionNotificationService auctionNotificationService;

    @Test
    @DisplayName("sendNotification은 Notification 생성 후 사용자 큐로 알림 페이로드를 전송한다")
    void sendNotification_createsAndPushesToQueue() {
        User user = user(1L);
        Notification saved = notification(user, 100L, NotificationType.IMMINENT);

        given(notificationService.create(1L, NotificationType.IMMINENT, "msg", "AUCTION", 10L)).willReturn(saved);

        auctionNotificationService.sendNotification(1L, NotificationType.IMMINENT, "msg", "AUCTION", 10L);

        verify(notificationService).create(1L, NotificationType.IMMINENT, "msg", "AUCTION", 10L);
        verify(auctionMessageService).sendToUserQueue(eq(1L), any(), any());
    }

    @Test
    @DisplayName("notifyImminent는 dedup key가 이미 있으면 아무 작업도 하지 않는다")
    void notifyImminent_returnsWhenDedupKeyExists() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), eq("1"), any())).willReturn(false);

        auctionNotificationService.notifyImminent(11L);

        verify(auctionRepository, never()).findById(any());
        verify(bidRepository, never()).findDistinctBidderIdsByAuctionId(any());
    }

    @Test
    @DisplayName("notifyImminent는 판매자와 참여자 모두에게 IMMINENT 알림을 보낸다")
    void notifyImminent_notifiesSellerAndParticipants() {
        Long auctionId = 22L;
        User seller = user(5L);
        Auction auction = auction(auctionId, seller);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(anyString(), eq("1"), any())).willReturn(true);
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));
        given(bidRepository.findDistinctBidderIdsByAuctionId(auctionId)).willReturn(List.of(7L, 8L));


        given(notificationService.create(
                anyLong(),
                eq(NotificationType.IMMINENT),
                eq(NotificationConstants.MSG_AUCTION_IMMINENT),
                eq(NotificationConstants.REF_TYPE_AUCTION),
                eq(auctionId)
        )).willAnswer(invocation -> {
            Long userId = invocation.getArgument(0, Long.class);
            return notification(user(userId), userId + 1000L, NotificationType.IMMINENT);
        });

        auctionNotificationService.notifyImminent(auctionId);

        verify(notificationService).create(
                seller.getId(),
                NotificationType.IMMINENT,
                NotificationConstants.MSG_AUCTION_IMMINENT,
                NotificationConstants.REF_TYPE_AUCTION,
                auctionId
        );
        verify(notificationService).create(
                7L,
                NotificationType.IMMINENT,
                NotificationConstants.MSG_AUCTION_IMMINENT,
                NotificationConstants.REF_TYPE_AUCTION,
                auctionId
        );
        verify(notificationService).create(
                8L,
                NotificationType.IMMINENT,
                NotificationConstants.MSG_AUCTION_IMMINENT,
                NotificationConstants.REF_TYPE_AUCTION,
                auctionId
        );

        verify(auctionMessageService).sendToUserQueue(eq(seller.getId()), any(), any());
        verify(auctionMessageService).sendToUserQueue(eq(7L), any(), any());
        verify(auctionMessageService).sendToUserQueue(eq(8L), any(), any());
    }

    private User user(Long id) {
        User user = User.builder()
                .email("u" + id + "@t.com")
                .nickname("u" + id)
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Auction auction(Long id, User seller) {
        Category category = new Category("cat" + id, null);
        ReflectionTestUtils.setField(category, "id", 1L + id);

        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title("title")
                .description("desc")
                .startPrice(BigDecimal.valueOf(1000))
                .build();
        ReflectionTestUtils.setField(item, "id", id + 1000);

        Auction auction = Auction.builder()
                .item(item)
                .startPrice(BigDecimal.valueOf(1000))
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusHours(1))
                .isHotDeal(false)
                .build();
        ReflectionTestUtils.setField(auction, "id", id);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.RUNNING);
        return auction;
    }

    private Notification notification(User user, Long id, NotificationType type) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .message("msg")
                .refType("AUCTION")
                .refId(10L)
                .build();
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }
}