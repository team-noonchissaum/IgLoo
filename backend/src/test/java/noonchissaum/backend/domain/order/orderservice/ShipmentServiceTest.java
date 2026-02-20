package noonchissaum.backend.domain.order.orderservice;

import noonchissaum.backend.domain.order.client.SweetTrackerClient;
import noonchissaum.backend.domain.order.dto.shipment.req.SaveAddressReq;
import noonchissaum.backend.domain.order.dto.shipment.req.ShipmentReq;
import noonchissaum.backend.domain.order.dto.shipment.res.ShipmentTrackingRes;
import noonchissaum.backend.domain.order.dto.shipment.res.SweetTrackerTrackingInfoRes;
import noonchissaum.backend.domain.order.entity.DeliveryType;
import noonchissaum.backend.domain.order.entity.Order;
import noonchissaum.backend.domain.order.entity.Shipment;
import noonchissaum.backend.domain.order.entity.ShipmentStatus;
import noonchissaum.backend.domain.order.repository.ShipmentRepository;
import noonchissaum.backend.domain.order.service.OrderService;
import noonchissaum.backend.domain.order.service.ShipmentService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ShipmentServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private SweetTrackerClient sweetTrackerClient;

    @InjectMocks
    private ShipmentService shipmentService;

    @Test
    @DisplayName("판매자가 아닌 사용자는 송장 등록을 할 수 없다")
    void registerTracking_deniesNonSeller() {
        Long orderId = 1L;
        User buyer = user(10L, "buyer@test.com", "buyer");
        User seller = user(20L, "seller@test.com", "seller");
        Order order = order(orderId, buyer, seller, DeliveryType.SHIPMENT);

        given(orderService.getOrder(orderId)).willReturn(order);

        assertThatThrownBy(() -> shipmentService.registerTracking(
                orderId, buyer.getId(), shipmentReq("04", "1234567890")
        ))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("배송지 정보가 없는 경우 송장 등록이 거부된다")
    void registerTracking_throwsWhenRequestInfoMissing() {
        Long orderId = 2L;
        User buyer = user(10L, "buyer@test.com", "buyer");
        User seller = user(20L, "seller@test.com", "seller");

        Order order = order(orderId, buyer, seller, DeliveryType.SHIPMENT);
        Shipment shipment = shipment(5L, order, ShipmentStatus.READY, false);

        given(orderService.getOrder(orderId)).willReturn(order);
        given(shipmentRepository.findByOrder_Id(orderId)).willReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.registerTracking(
                orderId, seller.getId(), shipmentReq("04", "1234567890")
        ))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SHIPMENT_REQUEST_NOT_FOUND);
    }

    @Test
    @DisplayName("송장 등록 시 운송장번호는 숫자만 남기고 SHIPPED 상태로 변경된다")
    void registerTracking_sanitizesTrackingNumberAndMarksShipped() {
        Long orderId = 3L;
        User buyer = user(10L, "buyer@test.com", "buyer");
        User seller = user(20L, "seller@test.com", "seller");

        Order order = order(orderId, buyer, seller, DeliveryType.SHIPMENT);
        Shipment shipment = shipment(6L, order, ShipmentStatus.READY, true);

        given(orderService.getOrder(orderId)).willReturn(order);
        given(shipmentRepository.findByOrder_Id(orderId)).willReturn(Optional.of(shipment));

        var result = shipmentService.registerTracking(orderId, seller.getId(), shipmentReq(" 04 ", "12-34 56ab"));

        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.SHIPPED.name());
        assertThat(result.getCarrierCode()).isEqualTo("04");
        assertThat(result.getTrackingNumber()).isEqualTo("123456");
        assertThat(shipment.getShippedAt()).isNotNull();
    }

    @Test
    @DisplayName("구매자가 아닌 사용자는 배송지 저장을 할 수 없다")
    void saveAddress_deniesNonBuyer() {
        Long orderId = 4L;
        User buyer = user(10L, "buyer@test.com", "buyer");
        User seller = user(20L, "seller@test.com", "seller");

        Order order = order(orderId, buyer, seller, DeliveryType.SHIPMENT);

        given(orderService.getOrder(orderId)).willReturn(order);
        // ❗중요: 여기서는 권한 체크에서 바로 throw라 shipmentRepository를 stub할 필요가 없음(=Unnecessary stubbing 방지)

        assertThatThrownBy(() -> shipmentService.saveAddress(orderId, seller.getId(), addressReq()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        verify(shipmentRepository, never()).findByOrder_Id(any());
    }

    @Test
    @DisplayName("운송장 조회에서 배송완료 이벤트가 오면 상태를 DELIVERED로 동기화한다")
    void getTracking_marksDeliveredWhenCarrierSaysDelivered() {
        Long orderId = 5L;
        User buyer = user(10L, "buyer@test.com", "buyer");
        User seller = user(20L, "seller@test.com", "seller");
        Order order = order(orderId, buyer, seller, DeliveryType.SHIPMENT);

        // 주소 저장은 READY에서 해야 IllegalStateException 안 남
        Shipment shipment = shipment(8L, order, ShipmentStatus.READY, true);

        //  테스트 목적상 SHIPPED 상태로 “전환”
        ReflectionTestUtils.setField(shipment, "status", ShipmentStatus.SHIPPED);

        // 송장정보 세팅 (canSyncTracking() 통과)
        ReflectionTestUtils.setField(shipment, "carrierCode", "04");
        ReflectionTestUtils.setField(shipment, "trackingNumber", "123456");

        given(shipmentRepository.findByOrder_Id(orderId)).willReturn(Optional.of(shipment));
        given(sweetTrackerClient.trackingInfo("04", "123456")).willReturn(
                new SweetTrackerTrackingInfoRes(
                        true,
                        null,
                        "123456",
                        "sender",
                        "receiver",
                        "item",
                        "배송완료",
                        "Y",
                        List.of(new SweetTrackerTrackingInfoRes.SweetTrackerTrackingDetail(
                                "2025-01-01 12:00",
                                "서울",
                                "배송완료",
                                "010-0000-0000"
                        ))
                )
        );

        ShipmentTrackingRes result = shipmentService.getTracking(orderId, buyer.getId());

        assertThat(result.delivered()).isTrue();
        assertThat(result.currentStatus()).isEqualTo("배송완료");
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(shipment.getDeliveredAt()).isNotNull();
    }

    @Test
    @DisplayName("송장 정보가 없으면 운송장 조회 외부 API를 호출하지 않는다")
    void getTracking_throwsWhenTrackingNotAvailable() {
        Long orderId = 6L;
        User buyer = user(10L, "buyer@test.com", "buyer");
        User seller = user(20L, "seller@test.com", "seller");
        Order order = order(orderId, buyer, seller, DeliveryType.SHIPMENT);

        // READY + 주소 있음이어도 carrier/tracking이 없으면 canSyncTracking() false일 가능성이 높음
        Shipment shipment = shipment(9L, order, ShipmentStatus.READY, true);

        given(shipmentRepository.findByOrder_Id(orderId)).willReturn(Optional.of(shipment));

        assertThatThrownBy(() -> shipmentService.getTracking(orderId, buyer.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SHIPMENT_TRACKING_NOT_AVAILABLE);

        verify(sweetTrackerClient, never()).trackingInfo(any(), any());
    }

    // -------------------- fixtures --------------------

    private User user(Long id, String email, String nickname) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Order order(Long orderId, User buyer, User seller, DeliveryType deliveryType) {
        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .deliveryType(deliveryType)
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    private Shipment shipment(Long shipmentId, Order order, ShipmentStatus status, boolean withAddress) {
        Shipment shipment = Shipment.builder()
                .order(order)
                .status(status)
                .build();
        ReflectionTestUtils.setField(shipment, "id", shipmentId);

        if (withAddress) {
            shipment.saveAddress("홍길동", "01012345678", "12345", "서울시", "101호", "문 앞");
        }
        return shipment;
    }

    private ShipmentReq shipmentReq(String carrierCode, String trackingNumber) {
        ShipmentReq req = new ShipmentReq();
        ReflectionTestUtils.setField(req, "carrierCode", carrierCode);
        ReflectionTestUtils.setField(req, "trackingNumber", trackingNumber);
        return req;
    }

    private SaveAddressReq addressReq() {
        return new SaveAddressReq("홍길동", "01012345678", "12345", "서울시", "101호", "문 앞");
    }
}
