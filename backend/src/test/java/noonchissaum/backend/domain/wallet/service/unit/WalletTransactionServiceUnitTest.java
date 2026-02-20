package noonchissaum.backend.domain.wallet.service.unit;

import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.wallet.dto.walletTransaction.res.WalletTransactionRes;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.entity.WalletTransaction;
import noonchissaum.backend.domain.wallet.repository.WalletTransactionRepository;
import noonchissaum.backend.domain.wallet.service.WalletTransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class WalletTransactionServiceUnitTest {

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Test
    @DisplayName("내 거래 내역 조회 시 페이지 결과를 응답 DTO로 매핑")
    void getMyWalletTransaction_mapsPageToResponse() {
        WalletTransactionService service = new WalletTransactionService(walletTransactionRepository);
        PageRequest pageable = PageRequest.of(0, 10);
        WalletTransaction tx = sampleTransaction(1L, 12L, BigDecimal.valueOf(5000), TransactionType.CHARGE, 90L);

        when(walletTransactionRepository.findByWallet_User_Id(12L, pageable))
                .thenReturn(new PageImpl<>(List.of(tx), pageable, 1));

        Page<WalletTransactionRes> result = service.getMyWalletTransaction(12L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).transaction_id()).isEqualTo(1L);
        assertThat(result.getContent().get(0).amount()).isEqualByComparingTo("5000");
        assertThat(result.getContent().get(0).type()).isEqualTo(TransactionType.CHARGE);
        verify(walletTransactionRepository).findByWallet_User_Id(12L, pageable);
    }

    private WalletTransaction sampleTransaction(Long txId, Long userId, BigDecimal amount, TransactionType type, Long refId) {
        User user = User.builder()
                .email("wallet-tx-service-unit-" + userId + "@test.com")
                .nickname("wallet_tx_service_unit_" + userId)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.valueOf(100000))
                .lockedBalance(BigDecimal.ZERO)
                .build();
        ReflectionTestUtils.setField(wallet, "id", userId + 1000);

        WalletTransaction tx = WalletTransaction.create(wallet, amount, type, refId);
        ReflectionTestUtils.setField(tx, "id", txId);
        return tx;
    }
}
