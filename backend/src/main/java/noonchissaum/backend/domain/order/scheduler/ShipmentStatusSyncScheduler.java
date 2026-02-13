package noonchissaum.backend.domain.order.scheduler;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import noonchissaum.backend.domain.order.entity.Shipment;
import noonchissaum.backend.domain.order.repository.ShipmentRepository;
import noonchissaum.backend.domain.order.client.SweetTrackerClient;
import noonchissaum.backend.domain.order.dto.shipment.res.SweetTrackerTrackingInfoRes;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentStatusSyncScheduler {

    private final ShipmentRepository shipmentRepository;
    private final SweetTrackerClient sweetTrackerClient;

    @Transactional
    @Scheduled(fixedDelay = 10 * 60 * 1000L) // 10분마다
    public void syncDeliveredStatus() {
        LocalDateTime now = LocalDateTime.now();

        // 한 번에 50건만 (요금/트래픽 보호)
        List<Shipment> targets = shipmentRepository.findDeliveredSyncTargets(PageRequest.of(0, 50));
        if (targets.isEmpty()) return;

        int updated = 0;
        int errors = 0;

        for (Shipment shipment : targets) {
            try {
                if (!shipment.canSyncTracking()) continue;

                SweetTrackerTrackingInfoRes r =
                        sweetTrackerClient.trackingInfo(shipment.getCarrierCode(), shipment.getTrackingNumber());

                if (r == null || Boolean.FALSE.equals(r.status())) {
                    errors++;
                    continue;
                }


                String currentStatus = null;
                if (r.trackingDetails() != null && !r.trackingDetails().isEmpty()) {
                    var last = r.trackingDetails().get(r.trackingDetails().size() - 1);
                    currentStatus = last.kind();
                }

                boolean deliveredByCarrier = isDelivered(r.complete(), currentStatus);

                if (deliveredByCarrier) {
                    shipment.markDelivered(now);
                    updated++;
                }
            } catch (Exception e) {
                errors++;
                log.warn("[ShipmentSync] fail shipmentId={}, orderId={}, msg={}",
                        shipment.getId(),
                        shipment.getOrder() != null ? shipment.getOrder().getId() : null,
                        e.getMessage());
            }
        }

        log.info("[ShipmentSync] targets={}, deliveredUpdated={}, errors={}",
                targets.size(), updated, errors);
    }

    private boolean isDelivered(String complete, String currentStatus) {
        if (complete != null) {
            String c = complete.trim().toLowerCase();
            if (c.equals("y") || c.equals("yes") || c.equals("true") || c.equals("complete")) return true;
        }
        return currentStatus != null && currentStatus.contains("배송완료");
    }
}