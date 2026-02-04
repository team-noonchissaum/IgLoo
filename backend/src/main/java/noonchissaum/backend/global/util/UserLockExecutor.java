package noonchissaum.backend.global.util;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class UserLockExecutor {

    private final RedissonClient redissonClient;

    /**
     * 단일 유저 락
     */
    public void withUserLock(Long userId, Runnable runnable) {
        withUserLocks(List.of(userId), () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 다중 유저 락
     */
    public void withUserLocks(List<Long> userIds, Runnable runnable) {
        withUserLocks(userIds, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Lock 안에서 작업한 값을 반환받아야 할 경우
     */

    public <T> T withUserLock(Long userId, Supplier<T> supplier) {
        return withUserLocks(List.of(userId), supplier);
    }

    /**
     * 다중 유저 락
     */
    public <T> T withUserLocks(List<Long> userIds, Supplier<T> supplier) {
        //
        List<Long> ids = userIds.stream()
                .filter(id -> id != null && id != -1L)
                .distinct()
                .sorted(Long::compareTo)
                .toList();

        List<RLock> locks = new ArrayList<>();
        try {
            for (Long uid : ids) {
                RLock lock = redissonClient.getLock(RedisKeys.userLock(uid));
                boolean locked = lock.tryLock(3, 5, TimeUnit.SECONDS);
                if (!locked) throw new ApiException(ErrorCode.LOCK_ACQUISITION);
                locks.add(lock);
            }
            return supplier.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ErrorCode.LOCK_ACQUISITION);

        } finally {
            for (int i = locks.size() - 1; i >= 0; i--) {
                RLock l = locks.get(i);
                if (l.isHeldByCurrentThread()) l.unlock();
            }
        }
    }
}
