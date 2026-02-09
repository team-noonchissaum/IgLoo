package noonchissaum.backend.domain.order.dto.delivery.res;
import noonchissaum.backend.domain.order.entity.DeliveryType;

public record ChooseDeliveryTypeRes(
        Long orderId,
        DeliveryType deliveryType,
        Long roomId
) {

}
