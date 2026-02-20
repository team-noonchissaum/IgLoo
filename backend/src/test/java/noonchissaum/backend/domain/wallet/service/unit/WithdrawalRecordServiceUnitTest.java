package noonchissaum.backend.domain.wallet.service.unit;

import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.wallet.dto.withdrawal.req.WithdrawalReq;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.entity.WalletTransaction;
import noonchissaum.backend.domain.wallet.entity.Withdrawal;
import noonchissaum.backend.domain.wallet.entity.WithdrawalStatus;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.domain.wallet.repository.WalletTransactionRepository;
import noonchissaum.backend.domain.wallet.repository.WithdrawalRepository;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.domain.wallet.service.WalletTransactionRecordService;
import noonchissaum.backend.domain.wallet.service.WithdrawalRecordService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class WithdrawalRecordServiceUnitTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WithdrawalRepository withdrawalRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private WalletTransactionRecordService walletTransactionRecordService;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private WalletService walletService;

    @Test
    @DisplayName("출금 요청 시 최소 금액 미만이면 WITHDRAW_MIN_AMOUNT 예외 던짐")
    void requestWithdrawalTx_whenAmountIsTooSmall_throwsApiException() {
        WithdrawalRecordService service = new WithdrawalRecordService(
                walletRepository,
                withdrawalRepository,
                redisTemplate,
                walletTransactionRecordService,
                walletTransactionRepository,
                walletService
        );

        ApiException ex = assertThrows(ApiException.class,
                () -> service.requestWithdrawalTx(1L, new WithdrawalReq(BigDecimal.valueOf(9000), "국민", "1234")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WITHDRAW_MIN_AMOUNT);
        verify(walletRepository, never()).findForUpdateByUserId(1L);
    }

    @Test
    @DisplayName("출금 요청 처리 시 지갑 차감, 출금 저장, 거래 내역 기록, Redis 동기화 적용")
    void requestWithdrawalTx_success_updatesWalletAndRedis() {
        WithdrawalRecordService service = new WithdrawalRecordService(
                walletRepository,
                withdrawalRepository,
                redisTemplate,
                walletTransactionRecordService,
                walletTransactionRepository,
                walletService
        );
        Wallet wallet = sampleWallet(2L, "request", BigDecimal.valueOf(50000), BigDecimal.ZERO);
        Withdrawal saved = Withdrawal.create(wallet, BigDecimal.valueOf(20000), BigDecimal.valueOf(1000), "신한", "1111");
        ReflectionTestUtils.setField(saved, "id", 10L);

        when(walletRepository.findForUpdateByUserId(2L)).thenReturn(Optional.of(wallet));
        when(withdrawalRepository.save(any())).thenReturn(saved);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);

        Long withdrawalId = service.requestWithdrawalTx(2L, new WithdrawalReq(BigDecimal.valueOf(20000), "신한", "1111"));

        assertThat(withdrawalId).isEqualTo(10L);
        assertThat(wallet.getBalance()).isEqualByComparingTo("29000");
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("21000");
        verify(walletTransactionRecordService)
                .record(eq(wallet), eq(TransactionType.WITHDRAW_REQUEST), eq(BigDecimal.valueOf(21000)), eq(10L));
        verify(walletService).getBalance(2L);
        verify(ops).increment("user:2:balance", 21000L);
        verify(ops).increment("user:2:lockedBalance", 21000L);
    }

    @Test
    @DisplayName("출금 승인 처리 시 REQUESTED 상태가 아니면 WITHDRAW_NOT_REQUESTED 예외 던짐")
    void confirmWithdrawalTx_whenStatusIsNotRequested_throwsApiException() {
        WithdrawalRecordService service = new WithdrawalRecordService(
                walletRepository,
                withdrawalRepository,
                redisTemplate,
                walletTransactionRecordService,
                walletTransactionRepository,
                walletService
        );
        Wallet wallet = sampleWallet(3L, "confirm", BigDecimal.valueOf(10000), BigDecimal.valueOf(21000));
        Withdrawal withdrawal = Withdrawal.create(wallet, BigDecimal.valueOf(20000), BigDecimal.valueOf(1000), "농협", "2222");
        ReflectionTestUtils.setField(withdrawal, "status", WithdrawalStatus.APPROVED);
        when(withdrawalRepository.findWithLockById(20L)).thenReturn(Optional.of(withdrawal));

        ApiException ex = assertThrows(ApiException.class, () -> service.confirmWithdrawalTx(20L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WITHDRAW_NOT_REQUESTED);
        verify(walletRepository, never()).findForUpdateByUserId(any());
    }

    @Test
    @DisplayName("출금 승인 처리 시 출금 상태와 요청 거래 내역을 승인으로 변경")
    void confirmWithdrawalTx_success_confirmsWithdrawalAndTransaction() {
        WithdrawalRecordService service = new WithdrawalRecordService(
                walletRepository,
                withdrawalRepository,
                redisTemplate,
                walletTransactionRecordService,
                walletTransactionRepository,
                walletService
        );
        Wallet wallet = sampleWallet(4L, "confirm-success", BigDecimal.valueOf(10000), BigDecimal.valueOf(21000));
        Withdrawal withdrawal = Withdrawal.create(wallet, BigDecimal.valueOf(20000), BigDecimal.valueOf(1000), "하나", "3333");
        ReflectionTestUtils.setField(withdrawal, "id", 30L);
        WalletTransaction tx = WalletTransaction.create(wallet, BigDecimal.valueOf(-21000), TransactionType.WITHDRAW_REQUEST, 30L);

        when(withdrawalRepository.findWithLockById(30L)).thenReturn(Optional.of(withdrawal));
        when(walletRepository.findForUpdateByUserId(4L)).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.findByTypeAndRefId(TransactionType.WITHDRAW_REQUEST, 30L))
                .thenReturn(Optional.of(tx));
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);

        service.confirmWithdrawalTx(30L);

        assertThat(withdrawal.getStatus()).isEqualTo(WithdrawalStatus.APPROVED);
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("0");
        assertThat(tx.getType()).isEqualTo(TransactionType.WITHDRAW_CONFIRM);
        verify(walletService).getBalance(4L);
        verify(ops).increment("user:4:lockedBalance", -21000L);
    }

    @Test
    @DisplayName("출금 거부 처리 시 출금 상태 변경, 지갑 복구, 거래 내역 기록, Redis 동기화 적용")
    void rejectWithdrawalTx_success_rejectsAndRollsBackWallet() {
        WithdrawalRecordService service = new WithdrawalRecordService(
                walletRepository,
                withdrawalRepository,
                redisTemplate,
                walletTransactionRecordService,
                walletTransactionRepository,
                walletService
        );
        Wallet wallet = sampleWallet(5L, "reject", BigDecimal.valueOf(10000), BigDecimal.valueOf(21000));
        Withdrawal withdrawal = Withdrawal.create(wallet, BigDecimal.valueOf(20000), BigDecimal.valueOf(1000), "우리", "4444");
        ReflectionTestUtils.setField(withdrawal, "id", 40L);
        when(withdrawalRepository.findWithLockById(40L)).thenReturn(Optional.of(withdrawal));
        when(walletRepository.findForUpdateByUserId(5L)).thenReturn(Optional.of(wallet));
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);

        service.rejectWithdrawalTx(40L);

        assertThat(withdrawal.getStatus()).isEqualTo(WithdrawalStatus.REJECTED);
        assertThat(wallet.getBalance()).isEqualByComparingTo("31000");
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("0");
        verify(walletTransactionRecordService)
                .record(eq(wallet), eq(TransactionType.WITHDRAW_REJECT), eq(BigDecimal.valueOf(21000)), eq(40L));
        verify(walletService).getBalance(5L);
        verify(ops).increment("user:5:balance", 21000L);
        verify(ops).increment("user:5:lockedBalance", -21000L);
    }

    private Wallet sampleWallet(Long userId, String suffix, BigDecimal balance, BigDecimal lockedBalance) {
        User user = User.builder()
                .email("withdraw-record-unit-" + suffix + "@test.com")
                .nickname("withdraw_record_unit_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(balance)
                .lockedBalance(lockedBalance)
                .build();
        ReflectionTestUtils.setField(wallet, "id", userId + 1000);
        return wallet;
    }
}
