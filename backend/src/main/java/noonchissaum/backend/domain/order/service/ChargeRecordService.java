package noonchissaum.backend.domain.order.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.order.entity.ChargeCheck;
import noonchissaum.backend.domain.order.entity.CheckStatus;
import noonchissaum.backend.domain.order.repositroy.ChargeCheckRepository;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ChargeRecordService {

    private final ChargeCheckRepository chargeCheckRepository;
    private final WalletRepository walletRepository;
    private final StringRedisTemplate redisTemplate;
    // DB 트랜잭션 영역 (여기서 DB 락이 실제로 걸림)
    @Transactional
    protected void confirmChargeTx(Long chargeCheckId, Long userId) {
        // ChargeCheck DB 락 (PESSIMISTIC_WRITE)
        ChargeCheck chargeCheck = chargeCheckRepository.findWithLockById(chargeCheckId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHARGE_CHECK_NOT_FOUND));

        // 권한 체크
        if (!chargeCheck.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        // 멱등 처리
        if (chargeCheck.getStatus() == CheckStatus.CHECKED) {
            return; // 이미 반영됨
        }
        if (chargeCheck.getStatus() == CheckStatus.CANCELED) {
            throw new ApiException(ErrorCode.CHARGE_CANCELED);
        }

        BigDecimal amount = chargeCheck.getPayment().getAmount();

        //Wallet DB 락 (PESSIMISTIC_WRITE)
        Wallet wallet = walletRepository.findForUpdateByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));

        // DB 반영
        wallet.charge(amount);

        // =========================
        // 4) DB 커밋 성공 후 Redis 잔액 반영 (afterCommit)
        // =========================
        registerAfterCommitRedisCharge(userId, amount);
    }

    private void registerAfterCommitRedisCharge(Long userId, BigDecimal amount) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 방어적 처리 (원래는 @Transactional 안이라 active여야 함)
            redisTemplate.opsForValue().increment(RedisKeys.userBalance(userId), amount.longValue());
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // =========================
                // Redis 반영 (이건 DB 락/트랜잭션과 별개, 단 afterCommit이라 "DB 확정 후" 실행)
                // =========================
                redisTemplate.opsForValue().increment(RedisKeys.userBalance(userId), amount.longValue());
            }
        });
    }
}
