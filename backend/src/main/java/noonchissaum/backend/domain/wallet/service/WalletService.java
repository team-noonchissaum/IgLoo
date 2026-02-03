package noonchissaum.backend.domain.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final StringRedisTemplate redisTemplate;
    private final WalletRepository walletRepository;

    /**입찰 처리*/
    public void processBidWallet(Long userId, Long previousBidderId, BigDecimal bidAmount, BigDecimal currentPrice ,Long auctionId, String requestId) {

        String userBalanceKey = RedisKeys.userBalance(userId);
        String userLockedBalanceKey = RedisKeys.userLockedBalance(userId);

        Long remain = redisTemplate.opsForValue().decrement(userBalanceKey, bidAmount.longValue());
        redisTemplate.opsForValue().increment(userLockedBalanceKey, bidAmount.longValue());
        if (remain < 0) {
            redisTemplate.opsForValue().increment(userBalanceKey, bidAmount.longValue());
            redisTemplate.opsForValue().decrement(userLockedBalanceKey, bidAmount.longValue());
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        if (previousBidderId != -1L) {
            String prevUserBalanceKey = RedisKeys.userBalance(previousBidderId);
            String prevUserLockedBalanceKey = RedisKeys.userLockedBalance(previousBidderId);

            redisTemplate.opsForValue().increment(prevUserBalanceKey, currentPrice.longValue());
            redisTemplate.opsForValue().decrement(prevUserLockedBalanceKey, currentPrice.longValue());
        }

    }

    @Transactional(readOnly = true)
    public void getBalance(Long userId) {
        String userBalanceKey = RedisKeys.userBalance(userId);
        String userLockedBalanceKey = RedisKeys.userLockedBalance(userId);

        if (!redisTemplate.hasKey(userBalanceKey) || !redisTemplate.hasKey(userLockedBalanceKey)) {
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
            redisTemplate.opsForValue().set(userBalanceKey, wallet.getBalance().toPlainString(), Duration.ofMinutes(30));
            redisTemplate.opsForValue().set(userLockedBalanceKey, wallet.getLockedBalance().toPlainString(), Duration.ofMinutes(30));
        }
    }

    @Transactional
    public Wallet createWallet(User user) {
        Wallet wallet = new Wallet(user);
        return walletRepository.save(wallet);
    }

    /**잔액 확인용 메서드*/
    @Transactional(readOnly = true)
    public BigDecimal getCurrentBalance(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
        return wallet.getBalance();
    }
    /**캐시삭제*/
    public void clearWalletCache(Long userId) {
        redisTemplate.delete(RedisKeys.userBalance(userId));
        redisTemplate.delete(RedisKeys.userLockedBalance(userId));
    }

    /**탈퇴 첫 시도인지 확인*/
    public boolean isFirstDeleteAttempt(Long userId) {
        String attemptKey = "user:delete:attempt:" + userId;
        return !Boolean.TRUE.equals(redisTemplate.hasKey(attemptKey));
    }

    /**탈퇴 시도 기록*/
    public void markDeleteAttempt(Long userId) {
        String attemptKey = "user:delete:attempt:" + userId;
        redisTemplate.opsForValue().set(attemptKey, "1", 10, TimeUnit.MINUTES);
    }

    /**탈퇴 시도 기록 삭제*/
    public void clearDeleteAttempt(Long userId) {
        String attemptKey = "user:delete:attempt:" + userId;
        redisTemplate.delete(attemptKey);
    }
}
