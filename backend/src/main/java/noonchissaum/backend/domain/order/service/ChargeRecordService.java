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
    private final PaymentService paymentService;
    // DB 트랜잭션 영역
    @Transactional
    protected void confirmChargeTx(Long chargeCheckId, Long userId) {
        // ChargeCheck DB 락
        ChargeCheck chargeCheck = chargeCheckRepository.findWithLockById(chargeCheckId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHARGE_CHECK_NOT_FOUND));

        // 권한 체크
        if (!chargeCheck.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND_CHARGE);
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
        //DB 커밋 성공 후 Redis 잔액 반영 (afterCommit)
        registerAfterCommitRedisCharge(userId, amount);
        chargeCheck.confirm();
    }


    public void cancelChargeTx(Long chargeCheckId, Long userId,String cancelReason) {
        ChargeCheck chargeCheck = chargeCheckRepository.findWithLockById(chargeCheckId)
                .orElseThrow(() -> new ApiException(ErrorCode.CHARGE_LOCK_ACQUISITION));

        if(!chargeCheck.getUser().getId().equals(userId)){
            throw new ApiException(ErrorCode.NOT_FOUND_CHARGE);
        }
        if(chargeCheck.getStatus().equals(CheckStatus.CANCELED)){
            return ;
        }
        if(chargeCheck.getStatus().equals(CheckStatus.CHECKED)){
            throw new ApiException(ErrorCode.CHARGE_CONFIRMED);
        }
        // todo: pg사에 환불 요청 로직 필요
        paymentService.cancelPayment(userId,"내맴",chargeCheckId);


        chargeCheck.cancel();
    }



    private void registerAfterCommitRedisCharge(Long userId, BigDecimal amount) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 방어적 처리
            redisTemplate.opsForValue().increment(RedisKeys.userBalance(userId), amount.longValue());
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Redis 반영
                redisTemplate.opsForValue().increment(RedisKeys.userBalance(userId), amount.longValue());
            }
        });
    }


}
