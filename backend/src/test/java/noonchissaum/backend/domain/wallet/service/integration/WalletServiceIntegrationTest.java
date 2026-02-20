package noonchissaum.backend.domain.wallet.service.integration;

import jakarta.persistence.EntityManager;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.wallet.dto.wallet.res.WalletRes;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.domain.wallet.service.WalletTransactionRecordService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.util.UserLockExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(WalletService.class)
@Tag("integration")
class WalletServiceIntegrationTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private WalletService walletService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;

    @MockitoBean
    private StringRedisTemplate redisTemplate;
    @MockitoBean
    private UserLockExecutor userLockExecutor;
    @MockitoBean
    private WalletTransactionRecordService walletTransactionRecordService;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        user = userRepository.save(User.builder()
                .email("wallet-it-" + suffix + "@test.com")
                .nickname("wallet_it_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

        wallet = walletRepository.save(Wallet.builder()
                .user(user)
                .balance(BigDecimal.valueOf(50000))
                .lockedBalance(BigDecimal.valueOf(3000))
                .build());
        user.registerWallet(wallet);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("현재 잔액 조회")
    void getCurrentBalance_returnsPersistedBalance() {
        BigDecimal balance = walletService.getCurrentBalance(user.getId());
        assertThat(balance).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("내 지갑 스냅샷 DTO 반환")
    void getMyWallet_returnsWalletSnapshot() {
        WalletRes res = walletService.getMyWallet(user.getId());

        assertThat(res.balance()).isEqualByComparingTo("50000");
        assertThat(res.lockedBalance()).isEqualByComparingTo("3000");
    }

    @Test
    @DisplayName("지갑 미존재 시 CANNOT_FIND_WALLET 예외 던짐")
    void getCurrentBalance_whenWalletMissing_throwsApiException() {
        ApiException ex = assertThrows(ApiException.class, () -> walletService.getCurrentBalance(-999L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FIND_WALLET);
    }
}
