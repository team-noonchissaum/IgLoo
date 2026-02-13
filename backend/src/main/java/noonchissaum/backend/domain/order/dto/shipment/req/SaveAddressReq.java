package noonchissaum.backend.domain.order.dto.shipment.req;

public record SaveAddressReq(
        String recipientName,
        String recipientPhone,
        String zipCode,
        String address1,
        String address2,
        String deliveryMemo
) {
}
