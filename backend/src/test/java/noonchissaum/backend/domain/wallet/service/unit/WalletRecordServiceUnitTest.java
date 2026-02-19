package noonchissaum.backend.domain.wallet.service.unit;

import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.domain.wallet.service.WalletRecordService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class WalletRecordServiceUnitTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletTransactionRecordService walletTransactionRecordService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private WalletService walletService;

    @Test
    @DisplayName("입찰 지갑 기록 저장 시 신규 입찰자 동결 및 이전 입찰자 환불 적용")
    void saveWalletRecord_withPreviousBidder_appliesBidAndRefund() {
        WalletRecordService service = new WalletRecordService(
                walletRepository, walletTransactionRecordService, redisTemplate, walletService);

        Wallet bidder = sampleWallet(1L, "bidder", BigDecimal.valueOf(70000), BigDecimal.ZERO);
        Wallet previous = sampleWallet(2L, "previous", BigDecimal.valueOf(10000), BigDecimal.valueOf(20000));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(bidder));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(previous));

        service.saveWalletRecord(1L, BigDecimal.valueOf(10000), 2L, BigDecimal.valueOf(10000), 55L);

        assertThat(bidder.getBalance()).isEqualByComparingTo("60000");
        assertThat(bidder.getLockedBalance()).isEqualByComparingTo("10000");
        assertThat(previous.getBalance()).isEqualByComparingTo("20000");
        assertThat(previous.getLockedBalance()).isEqualByComparingTo("10000");

        verify(walletTransactionRecordService)
                .record(eq(bidder), eq(TransactionType.BID_HOLD), eq(BigDecimal.valueOf(10000)), eq(55L));
        verify(walletTransactionRecordService)
                .record(eq(previous), eq(TransactionType.BID_RELEASE), eq(BigDecimal.valueOf(10000)), eq(55L));
    }

    @Test
    @DisplayName("입찰 지갑 기록 저장 시 이전 입찰자 없으면 신규 입찰자만 처리")
    void saveWalletRecord_withoutPreviousBidder_recordsOnlyCurrentBidder() {
        WalletRecordService service = new WalletRecordService(
                walletRepository, walletTransactionRecordService, redisTemplate, walletService);
        Wallet bidder = sampleWallet(3L, "only", BigDecimal.valueOf(50000), BigDecimal.ZERO);
        when(walletRepository.findByUserId(3L)).thenReturn(Optional.of(bidder));

        service.saveWalletRecord(3L, BigDecimal.valueOf(15000), -1L, BigDecimal.ZERO, 56L);

        assertThat(bidder.getBalance()).isEqualByComparingTo("35000");
        assertThat(bidder.getLockedBalance()).isEqualByComparingTo("15000");
        verify(walletRepository, never()).findByUserId(-1L);
        verify(walletTransactionRecordService)
                .record(eq(bidder), eq(TransactionType.BID_HOLD), eq(BigDecimal.valueOf(15000)), eq(56L));
    }

    @Test
    @DisplayName("차단 경매 환불 처리 시 DB 환불 및 Redis 잔액 동기화 적용")
    void refundBlockedAuctionBid_refundsWalletAndSyncsRedis() {
        WalletRecordService service = new WalletRecordService(
                walletRepository, walletTransactionRecordService, redisTemplate, walletService);
        Wallet blocked = sampleWallet(4L, "blocked", BigDecimal.valueOf(1000), BigDecimal.valueOf(20000));
        when(walletRepository.findByUserId(4L)).thenReturn(Optional.of(blocked));

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);

        service.refundBlockedAuctionBid(4L, BigDecimal.valueOf(5000), 88L);

        assertThat(blocked.getBalance()).isEqualByComparingTo("6000");
        assertThat(blocked.getLockedBalance()).isEqualByComparingTo("15000");
        verify(walletService).getBalance(4L);
        verify(ops).increment("user:4:balance", 5000L);
        verify(ops).increment("user:4:lockedBalance", -5000L);
        verify(walletTransactionRecordService)
                .record(eq(blocked), eq(TransactionType.BID_RELEASE), eq(BigDecimal.valueOf(5000)), eq(88L));
    }

    @Test
    @DisplayName("입찰 롤백 처리 시 차단 사용자 환불 및 이전 입찰자 재동결 적용")
    void rollbackWalletRecord_withPreviousBidder_appliesRollbackFlow() {
        WalletRecordService service = new WalletRecordService(
                walletRepository, walletTransactionRecordService, redisTemplate, walletService);
        Wallet blocked = sampleWallet(5L, "rollback-blocked", BigDecimal.valueOf(0), BigDecimal.valueOf(12000));
        Wallet previous = sampleWallet(6L, "rollback-previous", BigDecimal.valueOf(40000), BigDecimal.ZERO);
        when(walletRepository.findByUserId(5L)).thenReturn(Optional.of(blocked));
        when(walletRepository.findByUserId(6L)).thenReturn(Optional.of(previous));

        service.rollbackWalletRecord(
                5L, 6L, BigDecimal.valueOf(12000), BigDecimal.valueOf(12000), 99L);

        assertThat(blocked.getBalance()).isEqualByComparingTo("12000");
        assertThat(blocked.getLockedBalance()).isEqualByComparingTo("0");
        assertThat(previous.getBalance()).isEqualByComparingTo("28000");
        assertThat(previous.getLockedBalance()).isEqualByComparingTo("12000");
    }

    @Test
    @DisplayName("입찰 지갑 기록 저장 시 지갑 미존재이면 CANNOT_FIND_WALLET 예외 던짐")
    void saveWalletRecord_whenWalletMissing_throwsApiException() {
        WalletRecordService service = new WalletRecordService(
                walletRepository, walletTransactionRecordService, redisTemplate, walletService);
        when(walletRepository.findByUserId(100L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.saveWalletRecord(100L, BigDecimal.valueOf(1000), -1L, BigDecimal.ZERO, 1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FIND_WALLET);
        verify(walletTransactionRecordService, never()).record(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), anyLong());
    }

    private Wallet sampleWallet(Long userId, String suffix, BigDecimal balance, BigDecimal lockedBalance) {
        User user = User.builder()
                .email("wallet-record-unit-" + suffix + "@test.com")
                .nickname("wallet_record_unit_" + suffix)
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
