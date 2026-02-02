package noonchissaum.backend.domain.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.util.UserLockExecutor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final StringRedisTemplate redisTemplate;
    private final WalletRepository walletRepository;
    private final UserService userService;
    private final UserLockExecutor userLockExecutor;
    private final WalletTransactionRecordService walletTransactionRecordService;

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
    public Wallet createWallet(Long userId) {
        User user = userService.getUserByUserId(userId);
        Wallet wallet = new Wallet(user);
        return walletRepository.save(wallet);
    }

    @Transactional
    public void setAuctionDeposit(Long userId, Long auctionId, int amount, String caseName) {
        BigDecimal depositAmount = new BigDecimal(amount);
        userLockExecutor.withUserLock(userId, () -> {
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));

            // 경매 등록 보증금 잔액 확인
            if (wallet.getBalance().compareTo(depositAmount) < 0) {
                throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE);
            }

            switch (caseName) {
                case "set" -> {
                    wallet.auctionDeposit(depositAmount);
                    walletTransactionRecordService.record(wallet, TransactionType.DEPOSIT_LOCK, depositAmount, auctionId);
                }
                case "refund" -> {
                    wallet.auctionRefund(depositAmount);
                    walletTransactionRecordService.record(wallet, TransactionType.DEPOSIT_RETURN, depositAmount, auctionId);
                }
                default -> {}
            }
        });
    }
}
