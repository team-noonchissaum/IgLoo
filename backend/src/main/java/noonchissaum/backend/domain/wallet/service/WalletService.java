package noonchissaum.backend.domain.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.domain.wallet.dto.wallet.res.WalletRes;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.util.UserLockExecutor;
import org.springframework.context.ApplicationEventPublisher;
import noonchissaum.backend.domain.wallet.repository.WalletTransactionRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final StringRedisTemplate redisTemplate;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
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

//     /**사용 가능 잔액 조회*/
//     @Transactional(readOnly = true)
//     public BigDecimal getAvailableBalance(Long userId) {
//         Wallet wallet = walletRepository.findByUserId(userId)
//                 .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
//         return wallet.getBalance();
//     }
//     /**입찰중인 금액*/
//     @Transactional(readOnly = true)
//     public BigDecimal getLockedBalance(Long userId) {
//         Wallet wallet = walletRepository.findByUserId(userId)
//                 .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
//         return wallet.getLockedBalance();
//     }
//     /**전체 잔액 조회(balance+lockedBalance)*/
//     @Transactional(readOnly = true)
//     public BigDecimal getTotalBalance(Long userId) {
//         Wallet wallet = walletRepository.findByUserId(userId)
//                 .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
//         return wallet.getBalance().add(wallet.getLockedBalance());
//     }
//     /**입찰시 balance차감+lockedBalnce증가*/
//     @Transactional
//     public void lockBalance(Long userId, BigDecimal amount) {
//         Wallet wallet = walletRepository.findByUserId(userId)
//                 .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));

//         if (wallet.getBalance().compareTo(amount) < 0) {
//             throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE);
//         }

//         wallet.bid(amount); // Wallet Entity의 bid() 메서드 사용
//         walletRepository.save(wallet);

//         // Redis도 동시 업데이트
//         String userBalanceKey = RedisKeys.userBalance(userId);
//         String userLockedBalanceKey = RedisKeys.userLockedBalance(userId);
//         redisTemplate.opsForValue().set(userBalanceKey, wallet.getBalance().toPlainString(), Duration.ofMinutes(30));
//         redisTemplate.opsForValue().set(userLockedBalanceKey, wallet.getLockedBalance().toPlainString(), Duration.ofMinutes(30));
//     }

//     /**redis와 db동기화*/
//     @Transactional
//     public void unlockBalance(Long userId, BigDecimal amount) {
//         Wallet wallet = walletRepository.findByUserId(userId)
//                 .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));

//         if (wallet.getLockedBalance().compareTo(amount) < 0) {
//             throw new ApiException(ErrorCode.INSUFFICIENT_LOCKED_BALANCE);
//         }

//         wallet.bidCanceled(amount); // Wallet Entity의 bidCanceled() 메서드 사용
//         walletRepository.save(wallet);

//         // Redis도 동시 업데이트
//         String userBalanceKey = RedisKeys.userBalance(userId);
//         String userLockedBalanceKey = RedisKeys.userLockedBalance(userId);
//         redisTemplate.opsForValue().set(userBalanceKey, wallet.getBalance().toPlainString(), Duration.ofMinutes(30));
//         redisTemplate.opsForValue().set(userLockedBalanceKey, wallet.getLockedBalance().toPlainString(), Duration.ofMinutes(30));
//     }

//     /**낙찰시 lockedBalance에서 최종 차감*/
//     @Transactional
//     public void deductLockedBalance(Long userId, BigDecimal amount) {
//         Wallet wallet = walletRepository.findByUserId(userId)
//                 .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));

//         if (wallet.getLockedBalance().compareTo(amount) < 0) {
//             throw new ApiException(ErrorCode.INSUFFICIENT_LOCKED_BALANCE);
//         }

//         wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
//         walletRepository.save(wallet);

//         // Redis도 동시 업데이트
//         String userLockedBalanceKey = RedisKeys.userLockedBalance(userId);
//         redisTemplate.opsForValue().set(userLockedBalanceKey, wallet.getLockedBalance().toPlainString(), Duration.ofMinutes(30));
//     }

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

    public WalletRes getMyWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
        return WalletRes.from(wallet);
    }
}
