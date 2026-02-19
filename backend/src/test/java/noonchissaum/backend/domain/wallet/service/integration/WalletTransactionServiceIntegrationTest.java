package noonchissaum.backend.domain.wallet.service.integration;

import jakarta.persistence.EntityManager;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.wallet.dto.walletTransaction.res.WalletTransactionRes;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.entity.WalletTransaction;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.domain.wallet.repository.WalletTransactionRepository;
import noonchissaum.backend.domain.wallet.service.WalletTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(WalletTransactionService.class)
@Tag("integration")
class WalletTransactionServiceIntegrationTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private WalletTransactionService walletTransactionService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        user = userRepository.save(User.builder()
                .email("wallet-tx-it-" + suffix + "@test.com")
                .nickname("wallet_tx_it_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        wallet = walletRepository.save(Wallet.builder()
                .user(user)
                .balance(BigDecimal.valueOf(100000))
                .lockedBalance(BigDecimal.ZERO)
                .build());
        user.registerWallet(wallet);

        walletTransactionRepository.save(WalletTransaction.create(wallet, BigDecimal.valueOf(3000), TransactionType.CHARGE, 1000L));
        walletTransactionRepository.save(WalletTransaction.create(wallet, BigDecimal.valueOf(-2000), TransactionType.BID_HOLD, 1001L));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("내 거래 내역 조회 시 사용자 거래만 페이지로 반환")
    void getMyWalletTransaction_returnsMappedPage() {
        Page<WalletTransactionRes> result = walletTransactionService.getMyWalletTransaction(user.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allSatisfy(res ->
                assertThat(res.ref_type()).isNotNull()
        );
    }
}
