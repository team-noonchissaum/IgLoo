package noonchissaum.backend.domain.order.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.chat.service.ChatRoomService;
import noonchissaum.backend.domain.order.dto.delivery.req.ChooseDeliveryTypeReq;
import noonchissaum.backend.domain.order.dto.delivery.res.ChooseDeliveryTypeRes;
import noonchissaum.backend.domain.order.entity.*;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.order.event.OrderConfirmedEvent;
import noonchissaum.backend.domain.order.repository.OrderRepository;
import noonchissaum.backend.domain.order.repository.ShipmentRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
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

    private final ApplicationEventPublisher eventPublisher;

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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getBuyer() == null || !order.getBuyer().getId().equals(buyerId)) {
            throw new ApiException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        Shipment shipment = shipmentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_SHIPMENT_NOT_FOUND));

        if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
            throw new ApiException(ErrorCode.ORDER_NOT_DELIVERED_YET);
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new ApiException(ErrorCode.ORDER_ALREADY_CONFIRMED);
        }

        // 엔티티 메서드(너가 추가한 confirmAfterDelivered) 사용
        order.confirmAfterDelivered();
        // 정산 처리되도록 이벤트만 발행
        eventPublisher.publishEvent(new OrderConfirmedEvent(orderId));

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
            eventPublisher.publishEvent(new OrderConfirmedEvent(orderId));
            done++;
        }

        return done;
    }
    // 추가
    @Transactional
    public ChooseDeliveryTypeRes chooseDeliveryType(Long orderId, Long userId, ChooseDeliveryTypeReq req) {
        if (req == null || req.type() == null) {
            throw new ApiException(ErrorCode.DELIVERY_TYPE_REQUIRED);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getBuyer() == null || !order.getBuyer().getId().equals(userId)) {
            throw new ApiException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        if (order.getDeliveryType() != null) {
            throw new ApiException(ErrorCode.DELIVERY_TYPE_ALREADY_SELECTED);
        }

        order.chooseDeliveryType(req.type());

        Long roomId = null;
        if (req.type() == DeliveryType.DIRECT) {
            roomId = chatRoomService.createRoom(order).getRoomId();
        }

        return new ChooseDeliveryTypeRes(order.getId(), order.getDeliveryType(), roomId);
    }

    @Transactional
    public void confirmDirectTrade(Long orderId, Long buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getBuyer() == null || !order.getBuyer().getId().equals(buyerId)) {
            throw new ApiException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        if (order.getDeliveryType() != DeliveryType.DIRECT) {
            throw new ApiException(ErrorCode.DELIVERY_TYPE_NOT_DIRECT);
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new ApiException(ErrorCode.ORDER_ALREADY_CONFIRMED);
        }
        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.SHIPPED) {
            throw new ApiException(ErrorCode.ORDER_CONFIRM_INVALID_STATUS);
        }

        order.confirmDirectTrade();

        // 직거래도 구매확정 개념이면 이벤트 발행
        eventPublisher.publishEvent(new OrderConfirmedEvent(orderId));
    }



}
