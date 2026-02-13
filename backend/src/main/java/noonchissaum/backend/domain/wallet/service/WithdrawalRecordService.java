package noonchissaum.backend.domain.wallet.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.wallet.dto.withdrawal.req.WithdrawalReq;
import noonchissaum.backend.domain.wallet.entity.*;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.domain.wallet.repository.WalletTransactionRepository;
import noonchissaum.backend.domain.wallet.repository.WithdrawalRepository;
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
public class WithdrawalRecordService {

    private static final BigDecimal MIN_AMOUNT = BigDecimal.valueOf(10000);
    private static final BigDecimal FIXED_FEE = BigDecimal.valueOf(1000);

    private final WalletRepository walletRepository;
    private final WithdrawalRepository withdrawalRepository;

    private final StringRedisTemplate redisTemplate;
    private final WalletTransactionRecordService walletTransactionRecordService;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletService walletService;

    /**
     * 출금 요청
     * balance -- , lockedBalance ++
     */
    @Transactional
    public Long requestWithdrawalTx(Long userId, WithdrawalReq req) {
        if (req.amount() == null || req.amount().compareTo(MIN_AMOUNT) < 0) {
            throw new ApiException(ErrorCode.WITHDRAW_MIN_AMOUNT);
        }

        Wallet wallet = walletRepository.findForUpdateByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));

        BigDecimal total = req.amount().add(FIXED_FEE);

        // DB 잔액 차감
        wallet.withdrawRequest(total);

        Withdrawal saved = withdrawalRepository.save(
                Withdrawal.create(wallet, req.amount(), FIXED_FEE, req.bankName(), req.accountNumber()));

        //지갑 내역에 추가
        walletTransactionRecordService.record(wallet, TransactionType.WITHDRAW_REQUEST,req.amount().add(FIXED_FEE), saved.getId());

        // Redis 반영
        registerAfterCommitRedisBalanceAndLockedDelta(userId, total, total);

        return saved.getId();
    }

    /**
     * 출금 승인
     * lockedBalance --
     */
    @Transactional
    public void confirmWithdrawalTx(Long withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findWithLockById(withdrawalId)
                .orElseThrow(() -> new ApiException(ErrorCode.WITHDRAW_NOT_FOUND));

        if (withdrawal.getStatus() != WithdrawalStatus.REQUESTED) {
            throw new ApiException(ErrorCode.WITHDRAW_NOT_REQUESTED);
        }

        Long userId = withdrawal.getWallet().getUser().getId();

        Wallet wallet = walletRepository.findForUpdateByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));

        BigDecimal total = withdrawal.getAmount().add(withdrawal.getFeeAmount());

        // DB: lockedBalance 만 감소
        wallet.withdrawConfirm(total);

        withdrawal.confirm();

        //지갑 내역에 승인으로 변경
        WalletTransaction walletTransaction = walletTransactionRepository.findByTypeAndRefId(TransactionType.WITHDRAW_REQUEST, withdrawal.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.WITHDRAW_NOT_FOUND));
        walletTransaction.confirmWithdrawal();

        // Redis 반영
        registerAfterCommitRedisBalanceAndLockedDelta(userId, BigDecimal.ZERO, total.negate());
    }

    /**
     * 출금 거부(반려)
     * balance ++ , lockedBalance --
     */
    @Transactional
    public void rejectWithdrawalTx(Long withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findWithLockById(withdrawalId)
                .orElseThrow(() -> new ApiException(ErrorCode.WITHDRAW_NOT_FOUND));

        if (withdrawal.getStatus() != WithdrawalStatus.REQUESTED) {
            throw new ApiException(ErrorCode.WITHDRAW_NOT_REQUESTED);
        }

        Long userId = withdrawal.getWallet().getUser().getId();

        Wallet wallet = walletRepository.findForUpdateByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));

        BigDecimal total = withdrawal.getAmount().add(withdrawal.getFeeAmount());

        // DB 묶인 돈 해제
        wallet.withdrawRollback(total);

        withdrawal.reject();

        //지갑 내역에 추가
        walletTransactionRecordService.record(wallet, TransactionType.WITHDRAW_REJECT,total, withdrawal.getId());

        //Redis 반영
        registerAfterCommitRedisBalanceAndLockedDelta(userId, total, total.negate());
    }


    /**
     * afterCommit Redis 반영 유틸 (키가 있을 때만 반영)
     * - user:{id}:balance / lockedBalance 키가 존재하는지 확인
     * - 키가 없으면 Redis에 새로 만들지 않기 위해 스킵
     */

    private void registerAfterCommitRedisBalanceAndLockedDelta(Long userId, BigDecimal balanceDelta, BigDecimal lockedDelta) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            applyRedisBalanceAndLockedDeltaIfPresent(userId, balanceDelta, lockedDelta);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                applyRedisBalanceAndLockedDeltaIfPresent(userId, balanceDelta, lockedDelta);
            }
        });
    }

    private void applyRedisBalanceAndLockedDeltaIfPresent(Long userId, BigDecimal balanceDelta, BigDecimal lockedDelta) {
        walletService.getBalance(userId);
        String balanceKey = RedisKeys.userBalance(userId);
        String lockedKey = RedisKeys.userLockedBalance(userId);

        long b = balanceDelta.longValue();
        long l = lockedDelta.longValue();

        if (b != 0) {
            redisTemplate.opsForValue().increment(balanceKey, b);
        }
        if (l != 0) {
            redisTemplate.opsForValue().increment(lockedKey, l);
        }
    }
}
