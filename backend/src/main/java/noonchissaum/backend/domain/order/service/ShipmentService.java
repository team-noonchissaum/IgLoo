package noonchissaum.backend.domain.order.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.order.client.SweetTrackerClient;
import noonchissaum.backend.domain.order.dto.shipment.req.ShipmentReq;
import noonchissaum.backend.domain.order.dto.shipment.res.ShipmentRes;
import noonchissaum.backend.domain.order.dto.shipment.res.ShipmentTrackingRes;
import noonchissaum.backend.domain.order.dto.shipment.res.SweetTrackerTrackingInfoRes;
import noonchissaum.backend.domain.order.entity.DeliveryType;
import noonchissaum.backend.domain.order.entity.Order;
import noonchissaum.backend.domain.order.entity.Shipment;
import noonchissaum.backend.domain.order.entity.ShipmentStatus;
import noonchissaum.backend.domain.order.repository.ShipmentRepository;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import noonchissaum.backend.domain.order.dto.shipment.req.SaveAddressReq;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipmentService {
    private final OrderService orderService;
    private final ShipmentRepository shipmentRepository;
    private final SweetTrackerClient sweetTrackerClient;

    @Transactional
    public ShipmentRes registerTracking(Long orderId, Long userId, ShipmentReq req) {
        // 1) 주문 조회
        Order order = orderService.getOrder(orderId);

        // 2) 판매자 권한
        if (order.getSeller() == null || !order.getSeller().getId().equals(userId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        // 3) 택배 거래만
        if (order.getDeliveryType() != DeliveryType.SHIPMENT) {
            throw new ApiException(ErrorCode.DELIVERY_TYPE_NOT_SHIPMENT);
        }

        // 4) 배송 엔티티 조회
        Shipment shipment = shipmentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.SHIPMENT_NOT_FOUND));

        // 5) 배송지(요청정보) 선행 필요
        if (!shipment.hasRequestInfo()) {
            throw new ApiException(ErrorCode.SHIPMENT_REQUEST_NOT_FOUND);
        }

        // 6) 요청값 검증 + 정규화
        if (req == null) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        String carrierCode = req.getCarrierCode() == null ? null : req.getCarrierCode().trim();
        String trackingNumber = req.getTrackingNumber() == null ? null : req.getTrackingNumber().replaceAll("[^0-9]", "");

        if (isBlank(carrierCode) || isBlank(trackingNumber)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }


        // 7) 이미 발송 처리된 경우 차단
        if (shipment.getStatus() == ShipmentStatus.SHIPPED || shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new ApiException(ErrorCode.SHIPMENT_ALREADY_SHIPPED);
        }

        // 8) 송장 입력
        shipment.inputInvoice(carrierCode, trackingNumber, LocalDateTime.now());

        return ShipmentRes.from(shipment);
    }



    @Transactional(readOnly = true)
    public ShipmentRes getShipment(Long orderId, Long userId){
        Shipment shipment = shipmentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.SHIPMENT_NOT_FOUND));

        Order order = shipment.getOrder();
        boolean isMember =
                (order.getBuyer() != null && order.getBuyer().getId().equals(userId)) ||
                (order.getSeller() != null && order.getSeller().getId().equals(userId));

        if (!isMember) throw new ApiException(ErrorCode.ACCESS_DENIED);

        return ShipmentRes.from(shipment);
    }
    @Transactional
    public ShipmentRes requestShipment(Long orderId){
        Shipment shipment = shipmentRepository.findByOrder_Id(orderId)
                .orElseGet(() -> {
                    Order order = orderService.getOrder(orderId);
                    return shipmentRepository.save(
                            Shipment.builder()
                                    .order(order)
                                    .status(ShipmentStatus.READY)
                                    .build()
                    );
                });
        return ShipmentRes.from(shipment);
    }

    @Transactional
    public ShipmentRes saveAddress(Long orderId, Long userId, SaveAddressReq req) {
        Order order = orderService.getOrder(orderId);

        // 구매자만
        if (order.getBuyer() == null || !order.getBuyer().getId().equals(userId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
        // SHIPMENT만
        if (order.getDeliveryType() != DeliveryType.SHIPMENT) {
            throw new ApiException(ErrorCode.DELIVERY_TYPE_NOT_SHIPMENT);
        }

        Shipment shipment = shipmentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.SHIPMENT_NOT_FOUND));

        shipment.saveAddress(
                req.recipientName(),
                req.recipientPhone(),
                req.zipCode(),
                req.address1(),
                req.address2(),
                req.deliveryMemo()
        );
        return ShipmentRes.from(shipment);
    }
    @Transactional
    public ShipmentTrackingRes getTracking(Long orderId, Long userId) {
        Shipment shipment = shipmentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.SHIPMENT_NOT_FOUND));

        Order order = shipment.getOrder();

        boolean isMember =
                (order.getBuyer() != null && order.getBuyer().getId().equals(userId)) ||
                        (order.getSeller() != null && order.getSeller().getId().equals(userId));

        if (!isMember) throw new ApiException(ErrorCode.ACCESS_DENIED);

        // 송장 입력 안됐으면 조회 불가
        if (!shipment.canSyncTracking()) {
            throw new ApiException(ErrorCode.SHIPMENT_TRACKING_NOT_AVAILABLE);
        }

        SweetTrackerTrackingInfoRes r =
                sweetTrackerClient.trackingInfo(shipment.getCarrierCode(), shipment.getTrackingNumber());

        if (r == null || Boolean.FALSE.equals(r.status())) {
            throw new ApiException(ErrorCode.SWEETTRACKER_API_ERROR);
        }

        var events = (r.trackingDetails() == null) ? List.<ShipmentTrackingRes.Event>of()
                : r.trackingDetails().stream()
                .map(d -> new ShipmentTrackingRes.Event(d.timeString(), d.where(), d.kind(), d.telno()))
                .toList();

        String currentStatus = events.isEmpty() ? null : events.get(events.size() - 1).kind();

        boolean deliveredByCarrier = isDelivered(r.complete(), currentStatus);

        // 택배사 기준 배송완료면 우리 시스템도 DELIVERED로 동기화 (한 번만)
        if (deliveredByCarrier && shipment.getStatus() == ShipmentStatus.SHIPPED) {
            shipment.markDelivered(LocalDateTime.now());
        }

        return new ShipmentTrackingRes(
                shipment.getCarrierCode(),
                shipment.getTrackingNumber(),
                deliveredByCarrier,
                currentStatus,
                shipment.getShippedAt(),
                shipment.getDeliveredAt(),
                events
        );
    }
    private boolean isDelivered(String complete, String currentStatus) {
        if (complete != null) {
            String c = complete.trim().toLowerCase();
            if (c.equals("y") || c.equals("yes") || c.equals("true") || c.equals("complete")) return true;
        }
        return currentStatus != null && currentStatus.contains("배송완료");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
