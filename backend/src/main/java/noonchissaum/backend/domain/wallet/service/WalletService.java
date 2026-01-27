package noonchissaum.backend.domain.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final StringRedisTemplate redisTemplate;
    private final WalletRepository walletRepository;
    private final ApplicationEventPublisher eventPublisher;


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

        if (previousBidderId != null && previousBidderId != -1L) {
            String prevUserBalanceKey = RedisKeys.userBalance(previousBidderId);
            String prevUserLockedBalanceKey = RedisKeys.userLockedBalance(previousBidderId);

            redisTemplate.opsForValue().increment(prevUserBalanceKey, currentPrice.longValue());
            redisTemplate.opsForValue().decrement(prevUserLockedBalanceKey, currentPrice.longValue());
        }

    }

    public void getBalance(Long userId) {
        String userBalanceKey = RedisKeys.userBalance(userId);
        String userLockedBalanceKey = RedisKeys.userLockedBalance(userId);

        if (!redisTemplate.hasKey(userBalanceKey) || !redisTemplate.hasKey(userLockedBalanceKey)) {
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
            redisTemplate.opsForValue().set(userBalanceKey, wallet.getBalance().toPlainString());
            redisTemplate.opsForValue().set(userLockedBalanceKey, wallet.getLockedBalance().toPlainString());
        }
    }
}
