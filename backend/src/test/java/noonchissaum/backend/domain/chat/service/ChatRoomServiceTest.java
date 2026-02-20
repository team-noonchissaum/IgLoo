package noonchissaum.backend.domain.chat.service;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.chat.dto.res.ChatRoomRes;
import noonchissaum.backend.domain.chat.entity.ChatRoom;
import noonchissaum.backend.domain.chat.repository.ChatRoomRepository;
import noonchissaum.backend.domain.notification.entity.NotificationType;
import noonchissaum.backend.domain.notification.service.NotificationService;
import noonchissaum.backend.domain.order.entity.DeliveryType;
import noonchissaum.backend.domain.order.entity.Order;
import noonchissaum.backend.domain.order.repository.OrderRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Test
    @DisplayName("이미 채팅방이 있으면 기존 방을 반환하고 새로 저장하지 않는다")
    void createRoom_returnsExistingRoomWhenAlreadyExists() {
        User buyer = user(10L, "buyer@test.com", "buyer", UserRole.USER);
        User seller = user(20L, "seller@test.com", "seller", UserRole.USER);
        Auction auction = auction(100L);

        Order order = order(auction, buyer, seller, null);
        ChatRoom existingRoom = chatRoom(1L, auction, buyer, seller);

        given(chatRoomRepository.findByAuctionId(auction.getId())).willReturn(Optional.of(existingRoom));

        ChatRoomRes result = chatRoomService.createRoom(order);

        assertThat(result.getRoomId()).isEqualTo(existingRoom.getId());
        assertThat(result.getMyUserId()).isEqualTo(buyer.getId());
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        verify(notificationService, never()).sendNotification(anyLong(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("구매자가 배송거래(SHIPMENT)를 선택한 주문은 채팅방 생성이 불가하다")
    void ensureRoomForAuction_throwsWhenBuyerSelectedShipment() {
        Long auctionId = 200L;
        Long buyerId = 10L;

        User buyer = user(buyerId, "buyer@test.com", "buyer", UserRole.USER);
        User seller = user(20L, "seller@test.com", "seller", UserRole.USER);
        Order shipmentOrder = order(auction(auctionId), buyer, seller, DeliveryType.SHIPMENT);

        given(orderRepository.findByAuction_IdAndBuyer_Id(auctionId, buyerId)).willReturn(Optional.of(shipmentOrder));

        assertThatThrownBy(() -> chatRoomService.ensureRoomForAuction(auctionId, buyerId))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @DisplayName("구매자가 아직 거래방식을 선택하지 않았으면 DIRECT로 설정 후 채팅방을 생성한다")
    void ensureRoomForAuction_createsRoomWhenBuyerDidNotChooseDeliveryType() {
        Long auctionId = 300L;
        Long buyerId = 10L;

        User buyer = user(buyerId, "buyer@test.com", "buyer", UserRole.USER);
        User seller = user(20L, "seller@test.com", "seller", UserRole.USER);
        Auction auction = auction(auctionId);

        Order order = order(auction, buyer, seller, null);
        ChatRoom savedRoom = chatRoom(99L, auction, buyer, seller);

        given(orderRepository.findByAuction_IdAndBuyer_Id(auctionId, buyerId)).willReturn(Optional.of(order));
        given(chatRoomRepository.findByAuctionId(auctionId)).willReturn(Optional.empty());
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(savedRoom);

        ChatRoomRes result = chatRoomService.ensureRoomForAuction(auctionId, buyerId);

        assertThat(order.getDeliveryType()).isEqualTo(DeliveryType.DIRECT);
        assertThat(result.getRoomId()).isEqualTo(savedRoom.getId());
        verify(notificationService).sendNotification(
                eq(seller.getId()),
                eq(NotificationType.CHAT_CREATED),
                anyString(),
                anyString(),
                eq(savedRoom.getId())
        );
    }

    @Test
    @DisplayName("참여자도 관리자도 아닌 사용자는 채팅방 상세 조회가 거부된다")
    void getRoom_deniesNonParticipantAndNonAdmin() {
        User buyer = user(10L, "buyer@test.com", "buyer", UserRole.USER);
        User seller = user(20L, "seller@test.com", "seller", UserRole.USER);
        ChatRoom room = chatRoom(7L, auction(700L), buyer, seller);

        given(chatRoomRepository.findById(room.getId())).willReturn(Optional.of(room));
        UserPrincipal outsider = new UserPrincipal(30L, "outsider@test.com", "pw", UserRole.USER);

        assertThatThrownBy(() -> chatRoomService.getRoom(room.getId(), outsider))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("관리자는 참여자가 아니어도 채팅방 상세 조회가 가능하다")
    void getRoom_allowsAdminEvenIfNotParticipant() {
        User buyer = user(10L, "buyer@test.com", "buyer", UserRole.USER);
        User seller = user(20L, "seller@test.com", "seller", UserRole.USER);
        ChatRoom room = chatRoom(8L, auction(800L), buyer, seller);

        given(chatRoomRepository.findById(room.getId())).willReturn(Optional.of(room));
        UserPrincipal admin = new UserPrincipal(99L, "admin@test.com", "pw", UserRole.ADMIN);

        ChatRoomRes result = chatRoomService.getRoom(room.getId(), admin);

        assertThat(result.getRoomId()).isEqualTo(room.getId());
        assertThat(result.getMyRole()).isEqualTo("BUYER");
        assertThat(result.getMyUserId()).isEqualTo(buyer.getId());
    }

    private User user(Long id, String email, String nickname, UserRole role) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .role(role)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Auction auction(Long id) {
        Auction auction = Auction.builder()
                .startPrice(null)
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusHours(1))
                .isHotDeal(false)
                .build();
        ReflectionTestUtils.setField(auction, "id", id);
        return auction;
    }

    private ChatRoom chatRoom(Long roomId, Auction auction, User buyer, User seller) {
        ChatRoom room = ChatRoom.builder()
                .auction(auction)
                .buyer(buyer)
                .seller(seller)
                .build();
        ReflectionTestUtils.setField(room, "id", roomId);
        return room;
    }

    private Order order(Auction auction, User buyer, User seller, DeliveryType deliveryType) {
        return Order.builder()
                .auction(auction)
                .buyer(buyer)
                .seller(seller)
                .deliveryType(deliveryType)
                .build();
    }
}