package noonchissaum.backend.domain.wallet.service.integration;

import jakarta.persistence.EntityManager;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.domain.wallet.repository.WalletTransactionRepository;
import noonchissaum.backend.domain.wallet.service.WalletRecordService;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.domain.wallet.service.WalletTransactionRecordService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({WalletRecordService.class, WalletTransactionRecordService.class})
@Tag("integration")
class WalletRecordServiceIntegrationTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private WalletRecordService walletRecordService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @MockitoBean
    private StringRedisTemplate redisTemplate;
    @MockitoBean
    private WalletService walletService;

    private User bidder;
    private User previous;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        bidder = userRepository.save(User.builder()
                .email("wallet-record-bidder-" + suffix + "@test.com")
                .nickname("wallet_record_bidder_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        previous = userRepository.save(User.builder()
                .email("wallet-record-prev-" + suffix + "@test.com")
                .nickname("wallet_record_prev_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

        Wallet bidderWallet = walletRepository.save(Wallet.builder()
                .user(bidder)
                .balance(BigDecimal.valueOf(50000))
                .lockedBalance(BigDecimal.ZERO)
                .build());
        bidder.registerWallet(bidderWallet);

        Wallet previousWallet = walletRepository.save(Wallet.builder()
                .user(previous)
                .balance(BigDecimal.valueOf(20000))
                .lockedBalance(BigDecimal.valueOf(10000))
                .build());
        previous.registerWallet(previousWallet);

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("입찰 지갑 기록 저장 시 지갑 잔액 변경 및 BID_HOLD/BID_RELEASE 거래 내역 저장")
    void saveWalletRecord_updatesWalletAndPersistsTransactions() {
        walletRecordService.saveWalletRecord(
                bidder.getId(),
                BigDecimal.valueOf(10000),
                previous.getId(),
                BigDecimal.valueOf(10000),
                700L
        );
        em.flush();
        em.clear();

        Wallet bidderWallet = walletRepository.findByUserId(bidder.getId()).orElseThrow();
        Wallet previousWallet = walletRepository.findByUserId(previous.getId()).orElseThrow();

        assertThat(bidderWallet.getBalance()).isEqualByComparingTo("40000");
        assertThat(bidderWallet.getLockedBalance()).isEqualByComparingTo("10000");
        assertThat(previousWallet.getBalance()).isEqualByComparingTo("30000");
        assertThat(previousWallet.getLockedBalance()).isEqualByComparingTo("0");

        assertThat(walletTransactionRepository.findByTypeAndRefId(TransactionType.BID_HOLD, 700L)).isPresent();
        assertThat(walletTransactionRepository.findByTypeAndRefId(TransactionType.BID_RELEASE, 700L)).isPresent();
    }

    @Test
    @DisplayName("차단 경매 환불 처리 시 지갑 환불 및 Redis 동기화 호출")
    void refundBlockedAuctionBid_updatesWalletAndSyncsRedis() {
        walletRecordService.refundBlockedAuctionBid(previous.getId(), BigDecimal.valueOf(5000), 701L);
        em.flush();
        em.clear();

        Wallet previousWallet = walletRepository.findByUserId(previous.getId()).orElseThrow();
        assertThat(previousWallet.getBalance()).isEqualByComparingTo("25000");
        assertThat(previousWallet.getLockedBalance()).isEqualByComparingTo("5000");
    }
}
