package noonchissaum.backend.domain.order.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.chat.service.ChatRoomService;
import noonchissaum.backend.domain.order.dto.delivery.req.ChooseDeliveryTypeReq;
import noonchissaum.backend.domain.order.dto.delivery.res.ChooseDeliveryTypeRes;
import noonchissaum.backend.domain.order.entity.*;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.order.repository.OrderRepository;
import noonchissaum.backend.domain.order.repository.ShipmentRepository;
import noonchissaum.backend.domain.settlement.service.SettlementService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;

    private final ChatRoomService chatRoomService;
    private final SettlementService settlementService;

    @Transactional
    public void createOrder(Auction auction, User buyer, BigDecimal finalPrice) {
        Order order = Order.builder()
                .auction(auction)
                .item(auction.getItem())
                .buyer(buyer)
                .seller(auction.getSeller())
                .status(OrderStatus.CREATED)
                .deliveryType(null)// 아직 선택전
                .finalPrice(finalPrice)
                .build();
        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId){
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));
    }
    /**
     * 관리자 통계용 - 날짜별 전체 거래 수
     */
    public long countByDate(LocalDate date) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getCreatedAt().toLocalDate().equals(date))
                .count();
    }

    /**
     * 관리자 통계용 - 날짜별 완료 거래 수
     */
    public long countCompletedByDate(LocalDate date) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .filter(o -> o.getCreatedAt().toLocalDate().equals(date))
                .count();
    }

    /**
     * 관리자 통계용 - 날짜별 취소 거래 수
     */
    public long countCanceledByDate(LocalDate date) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.CANCELED)
                .filter(o -> o.getCreatedAt().toLocalDate().equals(date))
                .count();
    }
    /** (구매자) 배송완료 후 구매확정 */
    @Transactional
    public void confirmAfterDelivered(Long orderId, Long buyerId) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new IllegalArgumentException("권한 없음 또는 주문 없음"));

        Shipment shipment = shipmentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new IllegalStateException("배송 정보 없음"));

        if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
            throw new IllegalStateException("배송완료 후에만 구매확정이 가능합니다.");
        }

        // 엔티티 메서드(너가 추가한 confirmAfterDelivered) 사용
        order.confirmAfterDelivered();
        // 정산
        settlementService.settleOnOrderConfirmed(orderId);

    }

    /** (스케줄러) 배송완료 + 3일 자동확정 */
    @Transactional
    public int autoConfirmDeliveredOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(3);

        List<Long> targets = orderRepository.findAutoConfirmTargets(threshold);
        int done = 0;

        for (Long orderId : targets) {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) continue;

            if (order.getConfirmedAt() != null) continue;

            order.confirmAfterDelivered();
            settlementService.settleOnOrderConfirmed(orderId);

            done++;
        }

        return done;
    }
    // 추가
    @Transactional
    public ChooseDeliveryTypeRes chooseDeliveryType(Long orderId, Long userId, ChooseDeliveryTypeReq req) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED));

        order.chooseDeliveryType(req.type());

        Long roomId = null;
        if (req.type() == DeliveryType.DIRECT) {
            roomId = chatRoomService.createRoom(order).getRoomId();
        }

        return new ChooseDeliveryTypeRes(order.getId(), order.getDeliveryType(), roomId);
    }

    @Transactional
    public void confirmDirectTrade(Long orderId, Long buyerId) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, buyerId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED));

        if (order.getDeliveryType() != DeliveryType.DIRECT) {
            throw new ApiException(ErrorCode.DELIVERY_TYPE_NOT_DIRECT);
        }
        order.confirmDirectTrade();
    }


}
