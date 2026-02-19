package noonchissaum.backend.domain.wallet.service.integration;

import jakarta.persistence.EntityManager;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.UserRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({WithdrawalRecordService.class, WalletTransactionRecordService.class})
@Tag("integration")
class WithdrawalRecordServiceIntegrationTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private WithdrawalRecordService withdrawalRecordService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WithdrawalRepository withdrawalRepository;
    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @MockitoBean
    private StringRedisTemplate redisTemplate;
    @MockitoBean
    private WalletService walletService;

    private User user;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        user = userRepository.save(User.builder()
                .email("withdraw-record-it-" + suffix + "@test.com")
                .nickname("withdraw_record_it_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        Wallet wallet = walletRepository.save(Wallet.builder()
                .user(user)
                .balance(BigDecimal.valueOf(100000))
                .lockedBalance(BigDecimal.ZERO)
                .build());
        user.registerWallet(wallet);

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("출금 요청 처리 시 출금/거래 내역 저장 및 지갑 잔액-락 반영")
    void requestWithdrawalTx_persistsWithdrawalAndTransaction() {
        Long withdrawalId = withdrawalRecordService.requestWithdrawalTx(
                user.getId(),
                new WithdrawalReq(BigDecimal.valueOf(30000), "국민", "111-222")
        );
        em.flush();
        em.clear();

        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId).orElseThrow();
        Wallet wallet = walletRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(withdrawal.getStatus()).isEqualTo(WithdrawalStatus.REQUESTED);
        assertThat(wallet.getBalance()).isEqualByComparingTo("69000");
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("31000");
        assertThat(walletTransactionRepository.findByTypeAndRefId(TransactionType.WITHDRAW_REQUEST, withdrawalId)).isPresent();
    }

    @Test
    @DisplayName("출금 승인 처리 시 출금 상태 APPROVED 및 거래 내역 WITHDRAW_CONFIRM 적용")
    void confirmWithdrawalTx_confirmsWithdrawalAndTransaction() {
        Long withdrawalId = withdrawalRecordService.requestWithdrawalTx(
                user.getId(),
                new WithdrawalReq(BigDecimal.valueOf(20000), "신한", "333-444")
        );
        em.flush();
        em.clear();

        withdrawalRecordService.confirmWithdrawalTx(withdrawalId);
        em.flush();
        em.clear();

        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId).orElseThrow();
        Wallet wallet = walletRepository.findByUserId(user.getId()).orElseThrow();
        WalletTransaction tx = walletTransactionRepository
                .findByTypeAndRefId(TransactionType.WITHDRAW_CONFIRM, withdrawalId)
                .orElseThrow();

        assertThat(withdrawal.getStatus()).isEqualTo(WithdrawalStatus.APPROVED);
        assertThat(wallet.getBalance()).isEqualByComparingTo("79000");
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("0");
        assertThat(tx.getType()).isEqualTo(TransactionType.WITHDRAW_CONFIRM);
    }

    @Test
    @DisplayName("출금 거부 처리 시 출금 상태 REJECTED 및 잔액 복구 거래 내역 저장")
    void rejectWithdrawalTx_rejectsAndRollsBackWallet() {
        Long withdrawalId = withdrawalRecordService.requestWithdrawalTx(
                user.getId(),
                new WithdrawalReq(BigDecimal.valueOf(25000), "하나", "555-666")
        );
        em.flush();
        em.clear();

        withdrawalRecordService.rejectWithdrawalTx(withdrawalId);
        em.flush();
        em.clear();

        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId).orElseThrow();
        Wallet wallet = walletRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(withdrawal.getStatus()).isEqualTo(WithdrawalStatus.REJECTED);
        assertThat(wallet.getBalance()).isEqualByComparingTo("100000");
        assertThat(wallet.getLockedBalance()).isEqualByComparingTo("0");
        assertThat(walletTransactionRepository.findByTypeAndRefId(TransactionType.WITHDRAW_REJECT, withdrawalId)).isPresent();
    }

    @Test
    @DisplayName("출금 요청 처리 시 최소 금액 미만이면 WITHDRAW_MIN_AMOUNT 예외 던짐")
    void requestWithdrawalTx_whenAmountTooSmall_throwsApiException() {
        ApiException ex = assertThrows(ApiException.class,
                () -> withdrawalRecordService.requestWithdrawalTx(
                        user.getId(), new WithdrawalReq(BigDecimal.valueOf(5000), "우리", "777-888")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.WITHDRAW_MIN_AMOUNT);
    }
}
