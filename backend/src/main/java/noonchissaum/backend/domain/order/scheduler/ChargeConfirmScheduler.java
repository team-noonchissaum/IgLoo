package noonchissaum.backend.domain.order.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.order.entity.CheckStatus;
import noonchissaum.backend.domain.order.repository.ChargeCheckRepository;
import noonchissaum.backend.domain.order.service.ChargeRecordService;
import noonchissaum.backend.domain.task.service.TaskService;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.util.UserLockExecutor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChargeConfirmScheduler {

    private final ChargeCheckRepository chargeCheckRepository;
    private final ChargeRecordService chargeRecordService;
    private final TaskService taskService;
    private final UserLockExecutor userLockExecutor;

    private static final int BATCH_SIZE = 100;

    @Scheduled(cron = "10 0 0 * * *")
    public void autoConfirmExpiredCharges() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);

        List<Long> expiredIds = chargeCheckRepository.findExpiredUncheckedIds(
                CheckStatus.UNCHECKED,
                threshold,
                PageRequest.of(0, BATCH_SIZE)
        );

        if (expiredIds.isEmpty()) {
            return;
        }

        log.info("[AutoConfirm] expired unchecked size={}", expiredIds.size());

        for (Long chargeCheckId : expiredIds) {
            try {
                Long userId = chargeCheckRepository.findUser_IdById(chargeCheckId);
                if (userId == null) {
                    log.error("[AutoConfirm] charge check id not found");
                    continue;
                }

                try {
                    userLockExecutor.withUserLock(userId, () -> {
                        // 정합성 게이트
                        if (!taskService.checkTasks(userId)) return;

                        // DB 트랜잭션 + DB 락
                        chargeRecordService.confirmChargeTx(chargeCheckId, userId);
                    });
                } catch (Exception e) {
                    continue;
                }

            } catch (Exception e) {
                log.error("[AutoConfirm] failed chargeCheckId={}", chargeCheckId, e);
            }
        }
    }
}