package noonchissaum.backend.domain.order.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.order.dto.shipment.req.ShipmentReq;
import noonchissaum.backend.domain.order.dto.shipment.res.ShipmentRes;
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

@Service
@RequiredArgsConstructor
public class ShipmentService {
    private final OrderService orderService;
    private final ShipmentRepository shipmentRepository;

    @Transactional
    public ShipmentRes registerTracking(Long orderId, Long userId, ShipmentReq req){
        Order order = orderService.getOrder(orderId);

        if (order.getSeller() == null || !order.getSeller().getId().equals(userId)){
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        if (order.getDeliveryType() != DeliveryType.SHIPMENT){
            throw new ApiException(ErrorCode.DELIVERY_TYPE_NOT_SHIPMENT);
        }

        Shipment shipment = shipmentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.SHIPMENT_NOT_FOUND));

        if (!shipment.hasRequestInfo()) {
            throw new ApiException(ErrorCode.SHIPMENT_REQUEST_NOT_FOUND);
        }

        if (req == null || isBlank(req.getCarrierCode()) || isBlank(req.getTrackingNumber())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
        }

        // 이미 송장 등록된 경우 멱등/차단 정책 선택
        if (shipment.getStatus() == ShipmentStatus.SHIPPED || shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new ApiException(ErrorCode.SHIPMENT_ALREADY_SHIPPED);
        }

        shipment.inputInvoice(req.getCarrierCode(), req.getTrackingNumber(), LocalDateTime.now());
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

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
