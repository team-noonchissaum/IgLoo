package noonchissaum.backend.domain.order.dto.delivery.req;
import noonchissaum.backend.domain.order.entity.DeliveryType;

public record ChooseDeliveryTypeReq(
        DeliveryType type
) {
}
