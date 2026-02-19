package noonchissaum.backend.domain.wallet.service.integration;

import jakarta.persistence.EntityManager;
import noonchissaum.backend.domain.task.service.TaskService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.wallet.dto.withdrawal.res.WithdrawalRes;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.entity.Withdrawal;
import noonchissaum.backend.domain.wallet.entity.WithdrawalStatus;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.domain.wallet.repository.WithdrawalRepository;
import noonchissaum.backend.domain.wallet.service.WithdrawalRecordService;
import noonchissaum.backend.domain.wallet.service.WithdrawalService;
import noonchissaum.backend.global.util.UserLockExecutor;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(WithdrawalService.class)
@Tag("integration")
class WithdrawalServiceIntegrationTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private WithdrawalService withdrawalService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WithdrawalRepository withdrawalRepository;

    @MockitoBean
    private UserLockExecutor userLockExecutor;
    @MockitoBean
    private TaskService taskService;
    @MockitoBean
    private WithdrawalRecordService withdrawalRecordService;

    private User user;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        user = userRepository.save(User.builder()
                .email("withdraw-it-" + suffix + "@test.com")
                .nickname("withdraw_it_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

        Wallet wallet = walletRepository.save(Wallet.builder()
                .user(user)
                .balance(BigDecimal.valueOf(90000))
                .lockedBalance(BigDecimal.ZERO)
                .build());
        user.registerWallet(wallet);

        withdrawalRepository.save(Withdrawal.create(
                wallet,
                BigDecimal.valueOf(30000),
                BigDecimal.valueOf(1000),
                "국민",
                "1002-1234"
        ));

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("내 출금 신청 목록 페이징 조회")
    void getMyWithdrawals_returnsOnlyUserWithdrawals() {
        Page<WithdrawalRes> result = withdrawalService.getMyWithdrawals(user.getId(), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).amount()).isEqualByComparingTo("30000");
        assertThat(result.getContent().get(0).status()).isEqualTo(WithdrawalStatus.REQUESTED);
    }

    @Test
    @DisplayName("REQUESTED 상태 출금 목록 조회")
    void getRequestedWithdrawals_returnsRequestedOnly() {
        Page<WithdrawalRes> result = withdrawalService.getRequestedWithdrawals(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(result.getContent()).allSatisfy(res ->
                assertThat(res.status()).isEqualTo(WithdrawalStatus.REQUESTED)
        );
    }
}
