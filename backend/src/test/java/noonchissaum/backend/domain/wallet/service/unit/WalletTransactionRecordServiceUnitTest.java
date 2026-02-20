package noonchissaum.backend.domain.wallet.service.unit;

import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.entity.WalletTransaction;
import noonchissaum.backend.domain.wallet.repository.WalletTransactionRepository;
import noonchissaum.backend.domain.wallet.service.WalletTransactionRecordService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class WalletTransactionRecordServiceUnitTest {

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Test
    @DisplayName("거래 내역 기록 시 타입 부호 적용 후 저장")
    void record_appliesTransactionSignAndSaves() {
        WalletTransactionRecordService service = new WalletTransactionRecordService(walletTransactionRepository);
        Wallet wallet = sampleWallet(1L, "record-unit");

        service.record(wallet, TransactionType.BID_HOLD, BigDecimal.valueOf(10000), 77L);

        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(captor.capture());

        WalletTransaction saved = captor.getValue();
        assertThat(saved.getWallet()).isEqualTo(wallet);
        assertThat(saved.getType()).isEqualTo(TransactionType.BID_HOLD);
        assertThat(saved.getAmount()).isEqualByComparingTo("-10000");
        assertThat(saved.getRefId()).isEqualTo(77L);
    }

    @Test
    @DisplayName("거래 내역 기록 시 amount가 0 이하이면 INVALID_INPUT_VALUE 예외 던짐")
    void record_whenAmountIsNotPositive_throwsApiException() {
        WalletTransactionRecordService service = new WalletTransactionRecordService(walletTransactionRepository);
        Wallet wallet = sampleWallet(2L, "record-unit-invalid");

        ApiException ex = assertThrows(ApiException.class,
                () -> service.record(wallet, TransactionType.CHARGE, BigDecimal.ZERO, 11L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verifyNoInteractions(walletTransactionRepository);
    }

    private Wallet sampleWallet(Long userId, String suffix) {
        User user = User.builder()
                .email("wallet-tx-record-" + suffix + "@test.com")
                .nickname("wallet_tx_record_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.valueOf(100000))
                .lockedBalance(BigDecimal.valueOf(0))
                .build();
        ReflectionTestUtils.setField(wallet, "id", userId + 1000);
        return wallet;
    }
}
