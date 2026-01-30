package noonchissaum.backend.global.util;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class UserLockExecutor {

    private final RedissonClient redissonClient;

    public void withUserLock(Long userId, Runnable runnable) {
        RLock lock = redissonClient.getLock(RedisKeys.userLock(userId));
        boolean locked = false;

        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new ApiException(ErrorCode.LOCK_ACQUISITION);
            }

            runnable.run();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);

        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
