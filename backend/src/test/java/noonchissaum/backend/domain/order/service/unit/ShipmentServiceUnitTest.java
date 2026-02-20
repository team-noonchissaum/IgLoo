package noonchissaum.backend.domain.order.service.unit;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.order.client.SweetTrackerClient;
import noonchissaum.backend.domain.order.dto.shipment.req.ShipmentReq;
import noonchissaum.backend.domain.order.dto.shipment.res.ShipmentRes;
import noonchissaum.backend.domain.order.dto.shipment.res.ShipmentTrackingRes;
import noonchissaum.backend.domain.order.dto.shipment.res.SweetTrackerTrackingInfoRes;
import noonchissaum.backend.domain.order.entity.DeliveryType;
import noonchissaum.backend.domain.order.entity.Order;
import noonchissaum.backend.domain.order.entity.OrderStatus;
import noonchissaum.backend.domain.order.entity.Shipment;
import noonchissaum.backend.domain.order.entity.ShipmentStatus;
import noonchissaum.backend.domain.order.repository.ShipmentRepository;
import noonchissaum.backend.domain.order.service.OrderService;
import noonchissaum.backend.domain.order.service.ShipmentService;
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
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ShipmentServiceUnitTest {

    @Mock
    private OrderService orderService;
    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private SweetTrackerClient sweetTrackerClient;

    @Test
    @DisplayName("판매자가 아니면 송장 등록 시 ACCESS_DENIED 예외")
    void registerTracking_whenNotSeller_throwsApiException() {
        ShipmentService service = new ShipmentService(orderService, shipmentRepository, sweetTrackerClient);
        Order order = sampleOrder(10L, DeliveryType.SHIPMENT);

        when(orderService.getOrder(10L)).thenReturn(order);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.registerTracking(10L, 999L, new ShipmentReq()));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("송장 등록 성공 시 숫자만 추출해 SHIPPED 상태로 변경")
    void registerTracking_success_normalizesTrackingNumber() {
        ShipmentService service = new ShipmentService(orderService, shipmentRepository, sweetTrackerClient);
        Order order = sampleOrder(20L, DeliveryType.SHIPMENT);
        Shipment shipment = Shipment.builder().order(order).status(ShipmentStatus.READY).build();
        ReflectionTestUtils.setField(shipment, "id", 88L);
        ReflectionTestUtils.setField(shipment, "createdAt", LocalDateTime.now().minusMinutes(10));
        shipment.saveAddress("홍길동", "010-0000-0000", "12345", "서울시", "101호", "문앞");

        ShipmentReq req = new ShipmentReq();
        ReflectionTestUtils.setField(req, "carrierCode", " 04 ");
        ReflectionTestUtils.setField(req, "trackingNumber", "123-45-678");

        when(orderService.getOrder(20L)).thenReturn(order);
        when(shipmentRepository.findByOrder_Id(20L)).thenReturn(Optional.of(shipment));

        ShipmentRes result = service.registerTracking(20L, 3L, req);

        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.SHIPPED.name());
        assertThat(result.getCarrierCode()).isEqualTo("04");
        assertThat(result.getTrackingNumber()).isEqualTo("12345678");
    }

    @Test
    @DisplayName("택배사 완료 응답이면 배송 상태를 DELIVERED로 동기화")
    void getTracking_whenDeliveredByCarrier_marksDelivered() {
        ShipmentService service = new ShipmentService(orderService, shipmentRepository, sweetTrackerClient);
        Order order = sampleOrder(30L, DeliveryType.SHIPMENT);
        Shipment shipment = Shipment.builder()
                .order(order)
                .status(ShipmentStatus.SHIPPED)
                .carrierCode("04")
                .trackingNumber("12345678")
                .shippedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(shipmentRepository.findByOrder_Id(30L)).thenReturn(Optional.of(shipment));
        when(sweetTrackerClient.trackingInfo("04", "12345678"))
                .thenReturn(new SweetTrackerTrackingInfoRes(
                        true,
                        "ok",
                        "12345678",
                        "sender",
                        "receiver",
                        "item",
                        "배송완료",
                        "Y",
                        List.of(new SweetTrackerTrackingInfoRes.SweetTrackerTrackingDetail(
                                "2026-02-01 10:00",
                                "서울",
                                "배송완료",
                                "02-0000-0000"
                        ))
                ));

        ShipmentTrackingRes result = service.getTracking(30L, 2L);

        assertThat(result.delivered()).isTrue();
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(shipment.getDeliveredAt()).isNotNull();
    }

    private Order sampleOrder(Long orderId, DeliveryType deliveryType) {
        User buyer = sampleUser(2L, "buyer");
        User seller = sampleUser(3L, "seller");
        Category category = new Category("shipment-cat", null);
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title("item")
                .description("desc")
                .build();
        ReflectionTestUtils.setField(item, "id", 15L);

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
                .email("shipment-service-" + suffix + "@test.com")
                .nickname("shipment_service_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}

