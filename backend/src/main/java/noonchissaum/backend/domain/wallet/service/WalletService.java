package noonchissaum.backend.domain.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.wallet.dto.wallet.res.WalletRes;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.util.UserLockExecutor;
import noonchissaum.backend.domain.wallet.repository.WalletTransactionRepository;
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
    private final WalletTransactionRepository walletTransactionRepository;
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
                case "forfeit" -> {
                    wallet.auctionDeposit(depositAmount);
                    walletTransactionRecordService.record(wallet, TransactionType.DEPOSIT_FORFEIT, depositAmount, auctionId);
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

    /**
     * 입찰 롤백 시 지갑 역처리 (차단 유저 제거)
     * - 차단 유저: locked → balance (환불)
     * - 이전 입찰자: balance → locked (재동결)
     */
    public void rollbackBidWallet(Long blockedUserId, Long previousBidderId,
                                  BigDecimal blockedUserRefundAmount, BigDecimal previousBidderRelockAmount) {
        // Redis 캐시 로드 (없으면 DB에서)
        getBalance(blockedUserId);
        if (previousBidderId != null && previousBidderId != -1L) {
            getBalance(previousBidderId);
        }

        // 1. 차단 유저 환불: lockedBalance 감소, balance 증가
        String blockedBalanceKey = RedisKeys.userBalance(blockedUserId);
        String blockedLockedKey = RedisKeys.userLockedBalance(blockedUserId);
        redisTemplate.opsForValue().increment(blockedBalanceKey, blockedUserRefundAmount.longValue());
        redisTemplate.opsForValue().decrement(blockedLockedKey, blockedUserRefundAmount.longValue());

        // 2. 이전 입찰자 재동결: balance 감소, lockedBalance 증가
        if (previousBidderId != null && previousBidderId != -1L) {
            String prevBalanceKey = RedisKeys.userBalance(previousBidderId);
            String prevLockedKey = RedisKeys.userLockedBalance(previousBidderId);
            Long remain = redisTemplate.opsForValue().decrement(prevBalanceKey, previousBidderRelockAmount.longValue());
            if (remain != null && remain < 0) {
                redisTemplate.opsForValue().increment(prevBalanceKey, previousBidderRelockAmount.longValue());
                throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE);
            }
            redisTemplate.opsForValue().increment(prevLockedKey, previousBidderRelockAmount.longValue());
        }
    }
}
