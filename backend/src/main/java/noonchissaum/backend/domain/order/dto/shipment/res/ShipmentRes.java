package noonchissaum.backend.domain.order.dto.shipment.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.order.entity.Shipment;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ShipmentRes {
    private Long shipmentId;
    private Long orderId;

    private String recipientName;
    private String recipientPhone;
    private String zipCode;
    private String address1;
    private String address2;
    private String deliveryMemo;


    private String carrierCode;
    private String trackingNumber;
    private String status;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;

    public static ShipmentRes from(Shipment s){
        return new ShipmentRes(
                s.getId(),
                s.getOrder().getId(),
                s.getRecipientName(),
                s.getRecipientPhone(),
                s.getZipCode(),
                s.getAddress1(),
                s.getAddress2(),
                s.getDeliveryMemo(),
                s.getCarrierCode(),
                s.getTrackingNumber(),
                s.getStatus().name(),
                s.getShippedAt(),
                s.getDeliveredAt(),
                s.getCreatedAt()
        );
    }
}
