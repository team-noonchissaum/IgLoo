package noonchissaum.backend.domain.order.service.unit;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.chat.dto.res.ChatRoomRes;
import noonchissaum.backend.domain.chat.service.ChatRoomService;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.order.dto.delivery.req.ChooseDeliveryTypeReq;
import noonchissaum.backend.domain.order.dto.delivery.res.ChooseDeliveryTypeRes;
import noonchissaum.backend.domain.order.entity.DeliveryType;
import noonchissaum.backend.domain.order.entity.Order;
import noonchissaum.backend.domain.order.entity.OrderStatus;
import noonchissaum.backend.domain.order.entity.Shipment;
import noonchissaum.backend.domain.order.entity.ShipmentStatus;
import noonchissaum.backend.domain.order.repository.OrderRepository;
import noonchissaum.backend.domain.order.repository.ShipmentRepository;
import noonchissaum.backend.domain.order.service.OrderService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private ChatRoomService chatRoomService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("배송완료 전 구매확정 요청 시 IllegalStateException")
    void confirmAfterDelivered_whenShipmentNotDelivered_throws() {
        OrderService service = new OrderService(orderRepository, shipmentRepository, chatRoomService, eventPublisher);
        Order order = sampleOrder(10L, DeliveryType.SHIPMENT);
        Shipment shipment = Shipment.builder().order(order).status(ShipmentStatus.READY).build();

        when(orderRepository.findByIdAndBuyerId(10L, 2L)).thenReturn(Optional.of(order));
        when(shipmentRepository.findByOrder_Id(10L)).thenReturn(Optional.of(shipment));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.confirmAfterDelivered(10L, 2L));

        assertThat(ex.getMessage()).contains("배송완료");
    }

    @Test
    @DisplayName("직거래 선택 시 채팅방 생성 후 응답에 roomId 포함")
    void chooseDeliveryType_direct_returnsRoomId() {
        OrderService service = new OrderService(orderRepository, shipmentRepository, chatRoomService, eventPublisher);
        Order order = sampleOrder(20L, null);

        when(orderRepository.findByIdAndBuyerId(20L, 2L)).thenReturn(Optional.of(order));
        when(chatRoomService.createRoom(order)).thenReturn(new ChatRoomRes(
                77L,
                1L,
                2L,
                3L,
                2L,
                "BUYER",
                3L,
                "SELLER",
                LocalDateTime.now()
        ));

        ChooseDeliveryTypeRes result = service.chooseDeliveryType(20L, 2L, new ChooseDeliveryTypeReq(DeliveryType.DIRECT));

        assertThat(result.orderId()).isEqualTo(20L);
        assertThat(result.deliveryType()).isEqualTo(DeliveryType.DIRECT);
        assertThat(result.roomId()).isEqualTo(77L);
        verify(chatRoomService).createRoom(order);
    }

    @Test
    @DisplayName("직거래 확정 API에서 배송 타입이 DIRECT가 아니면 예외")
    void confirmDirectTrade_whenDeliveryTypeIsNotDirect_throwsApiException() {
        OrderService service = new OrderService(orderRepository, shipmentRepository, chatRoomService, eventPublisher);
        Order order = sampleOrder(30L, DeliveryType.SHIPMENT);

        when(orderRepository.findByIdAndBuyerId(30L, 2L)).thenReturn(Optional.of(order));

        ApiException ex = assertThrows(ApiException.class, () -> service.confirmDirectTrade(30L, 2L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DELIVERY_TYPE_NOT_DIRECT);
    }

    private Order sampleOrder(Long orderId, DeliveryType deliveryType) {
        User buyer = sampleUser(2L, "buyer");
        User seller = sampleUser(3L, "seller");
        Category category = new Category("order-cat", null);
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title("item")
                .description("desc")
                .startPrice(BigDecimal.valueOf(10000))
                .build();
        ReflectionTestUtils.setField(item, "id", 5L);

        Auction auction = Auction.builder()
                .item(item)
                .startPrice(BigDecimal.valueOf(10000))
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(auction, "id", 1L);

        Order order = Order.builder()
                .auction(auction)
                .item(item)
                .buyer(buyer)
                .seller(seller)
                .status(OrderStatus.CREATED)
                .deliveryType(deliveryType)
                .finalPrice(BigDecimal.valueOf(10000))
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    private User sampleUser(Long userId, String suffix) {
        User user = User.builder()
                .email("order-service-" + suffix + "@test.com")
                .nickname("order_service_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
