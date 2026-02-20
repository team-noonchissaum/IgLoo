package noonchissaum.backend.domain.wallet.service.integration;

import jakarta.persistence.EntityManager;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.entity.WalletTransaction;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.domain.wallet.repository.WalletTransactionRepository;
import noonchissaum.backend.domain.wallet.service.WalletTransactionRecordService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(WalletTransactionRecordService.class)
@Tag("integration")
class WalletTransactionRecordServiceIntegrationTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private WalletTransactionRecordService walletTransactionRecordService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .email("wallet-tx-record-it-" + suffix + "@test.com")
                .nickname("wallet_tx_record_it_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        wallet = walletRepository.save(Wallet.builder()
                .user(user)
                .balance(BigDecimal.valueOf(100000))
                .lockedBalance(BigDecimal.ZERO)
                .build());
        user.registerWallet(wallet);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("거래 내역 저장 시 타입 부호가 반영된 금액으로 저장")
    void record_persistsSignedAmountByType() {
        walletTransactionRecordService.record(wallet, TransactionType.BID_HOLD, BigDecimal.valueOf(15000), 100L);
        em.flush();
        em.clear();

        WalletTransaction tx = walletTransactionRepository.findByTypeAndRefId(TransactionType.BID_HOLD, 100L)
                .orElseThrow();

        assertThat(tx.getAmount()).isEqualByComparingTo("-15000");
        assertThat(tx.getType()).isEqualTo(TransactionType.BID_HOLD);
    }

    @Test
    @DisplayName("거래 내역 저장 시 amount가 0 이하이면 INVALID_INPUT_VALUE 예외 던짐")
    void record_whenAmountIsNotPositive_throwsApiException() {
        ApiException ex = assertThrows(ApiException.class,
                () -> walletTransactionRecordService.record(wallet, TransactionType.CHARGE, BigDecimal.ZERO, 101L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
