package noonchissaum.backend.domain.order.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.order.entity.CheckStatus;
import noonchissaum.backend.domain.order.repositroy.ChargeCheckRepository;
import noonchissaum.backend.domain.order.service.ChargeRecordService;
import noonchissaum.backend.domain.task.service.TaskService;
import noonchissaum.backend.global.RedisKeys;
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
    private final RedissonClient redissonClient;

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
                if (userId == null) continue;

                // Redis 락
                RLock userLock = redissonClient.getLock(RedisKeys.userLock(userId));
                boolean locked = false;

                try {
                    locked = userLock.tryLock(1, 10, TimeUnit.SECONDS);
                    if (!locked) {
                        // 락 못잡으면 다음 스케줄에 다시 시도
                        continue;
                    }

                    // 정합성 체크
                    if (!taskService.checkTasks(userId)) {
                        continue;
                    }

                    chargeRecordService.confirmChargeTx(chargeCheckId, userId);

                } finally {
                    if (locked && userLock.isHeldByCurrentThread()) {
                        userLock.unlock();
                    }
                }

            } catch (InterruptedException e) {
                // tryLock 대기 중 인터럽트 발생 시
                Thread.currentThread().interrupt();
                log.warn("[AutoConfirm] interrupted chargeCheckId={}", chargeCheckId, e);
                return;

            } catch (Exception e) {
                log.error("[AutoConfirm] failed chargeCheckId={}", chargeCheckId, e);
            }
        }
    }
}
