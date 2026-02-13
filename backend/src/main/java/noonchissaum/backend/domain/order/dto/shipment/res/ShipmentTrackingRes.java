package noonchissaum.backend.domain.order.dto.shipment.res;

import java.time.LocalDateTime;
import java.util.List;

public record ShipmentTrackingRes(
        String carrierCode,
        String trackingNumber,
        boolean delivered,
        String currentStatus,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        List<Event> events
) {
    public record Event(
            String timeString,
            String where,
            String kind,
            String telno
    ) {}
}