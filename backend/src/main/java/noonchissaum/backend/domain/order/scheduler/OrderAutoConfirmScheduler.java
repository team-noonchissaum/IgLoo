package noonchissaum.backend.domain.order.scheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import noonchissaum.backend.domain.order.service.OrderService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoConfirmScheduler {
    private final OrderService orderService;

    @Transactional
    @Scheduled(cron = "0 10 0 * * *") // 매일 00:10
    public void run() {
        int updated = orderService.autoConfirmDeliveredOrders();
        if (updated > 0) {
            log.info("[AutoConfirm] updated={}", updated);
        }
    }
}
