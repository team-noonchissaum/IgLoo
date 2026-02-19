package noonchissaum.backend.domain.order.service.unit;

import noonchissaum.backend.domain.order.entity.ChargeCheck;
import noonchissaum.backend.domain.order.entity.CheckStatus;
import noonchissaum.backend.domain.order.entity.Payment;
import noonchissaum.backend.domain.order.entity.PgProvider;
import noonchissaum.backend.domain.order.repository.ChargeCheckRepository;
import noonchissaum.backend.domain.order.service.ChargeRecordService;
import noonchissaum.backend.domain.order.service.PaymentService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.domain.wallet.service.WalletTransactionRecordService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ChargeRecordServiceUnitTest {

    @Mock
    private ChargeCheckRepository chargeCheckRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private PaymentService paymentService;
    @Mock
    private WalletTransactionRecordService walletTransactionRecordService;
    @Mock
    private WalletService walletService;

    @Test
    @DisplayName("충전 확인 처리 시 지갑 충전, ChargeCheck 확인, 거래 내역 기록, Redis 반영")
    void confirmChargeTx_success_updatesWalletAndChargeCheck() {
        ChargeRecordService service = new ChargeRecordService(
                chargeCheckRepository, walletRepository, redisTemplate, paymentService, walletTransactionRecordService, walletService);
        ChargeCheck check = sampleChargeCheck(1L, 20000, CheckStatus.UNCHECKED);
        Wallet wallet = sampleWallet(1L, BigDecimal.valueOf(5000));
        when(chargeCheckRepository.findWithLockById(99L)).thenReturn(Optional.of(check));
        when(walletRepository.findForUpdateByUserId(1L)).thenReturn(Optional.of(wallet));
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);

        service.confirmChargeTx(99L, 1L);

        assertThat(check.getStatus()).isEqualTo(CheckStatus.CHECKED);
        assertThat(wallet.getBalance()).isEqualByComparingTo("25000");
        verify(walletService).getBalance(1L);
        verify(ops).increment("user:1:balance", 20000L);
        verify(walletTransactionRecordService).record(eq(wallet), eq(TransactionType.CHARGE), eq(BigDecimal.valueOf(20000)), eq(99L));
    }

    @Test
    @DisplayName("충전 취소 처리 시 이미 확인된 충전이면 CHARGE_CONFIRMED 예외 던짐")
    void cancelChargeTx_whenAlreadyChecked_throwsApiException() {
        ChargeRecordService service = new ChargeRecordService(
                chargeCheckRepository, walletRepository, redisTemplate, paymentService, walletTransactionRecordService, walletService);
        ChargeCheck check = sampleChargeCheck(2L, 10000, CheckStatus.CHECKED);
        when(chargeCheckRepository.findWithLockById(100L)).thenReturn(Optional.of(check));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.cancelChargeTx(100L, 2L, "취소 요청"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CHARGE_CONFIRMED);
    }

    private ChargeCheck sampleChargeCheck(Long userId, int amount, CheckStatus status) {
        User user = User.builder()
                .email("charge-record-unit-" + userId + "@test.com")
                .nickname("charge_record_unit_" + userId)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Payment payment = Payment.builder()
                .user(user)
                .amount(BigDecimal.valueOf(amount))
                .pgProvider(PgProvider.TOSS)
                .pgOrderId("pg-" + userId)
                .build();
        ChargeCheck check = ChargeCheck.builder().payment(payment).build();
        ReflectionTestUtils.setField(check, "id", 900L + userId);
        check.changeStatus(status);
        return check;
    }

    private Wallet sampleWallet(Long userId, BigDecimal balance) {
        User user = User.builder()
                .email("wallet-charge-record-" + userId + "@test.com")
                .nickname("wallet_charge_record_" + userId)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(balance)
                .lockedBalance(BigDecimal.ZERO)
                .build();
        ReflectionTestUtils.setField(wallet, "id", userId + 1000);
        return wallet;
    }
}
