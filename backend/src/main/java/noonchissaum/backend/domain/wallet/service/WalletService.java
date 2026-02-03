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
import org.springframework.context.ApplicationEventPublisher;
import noonchissaum.backend.domain.wallet.repository.WalletTransactionRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final StringRedisTemplate redisTemplate;
    private final WalletRepository walletRepository;
    private final UserService userService;
    private final WalletTransactionRepository walletTransactionRepository;

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

    /**
     * 관리자 통계용 - 날짜별 충전 금액 합계
     */
    public long sumChargeByDate(LocalDate date) {
        return walletTransactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.CHARGE)
                .filter(t -> t.getCreatedAt().toLocalDate().equals(date))
                .map(t -> t.getAmount().longValue())
                .reduce(0L, Long::sum);
    }

    /**
     * 관리자 통계용 - 날짜별 사용 금액 합계
     */
    public long sumUsedByDate(LocalDate date) {
        return walletTransactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.BID_HOLD)
                .filter(t -> t.getCreatedAt().toLocalDate().equals(date))
                .map(t -> t.getAmount().longValue())
                .reduce(0L, Long::sum);
    }

    /**
     * 관리자 통계용 - 날짜별 출금 금액 합계
     */
    public long sumWithdrawnByDate(LocalDate date) {
        return walletTransactionRepository.findAll().stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAW)
                .filter(t -> t.getCreatedAt().toLocalDate().equals(date))
                .map(t -> t.getAmount().longValue())
                .reduce(0L, Long::sum);
    }

}
