package noonchissaum.backend.domain.order.dto.shipment.req;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ShipmentReq {
    private String carrierCode  ;
    private String trackingNumber;
}
