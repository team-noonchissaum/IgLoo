package noonchissaum.backend.domain.wallet.service.unit;

import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.domain.wallet.repository.WalletTransactionRepository;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.domain.wallet.service.WalletTransactionRecordService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.util.UserLockExecutor;
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
import java.time.Duration;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class WalletServiceUnitTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private UserLockExecutor userLockExecutor;
    @Mock
    private WalletTransactionRecordService walletTransactionRecordService;

    @Test
    @DisplayName("Redis 캐시 미스 시 지갑 조회 후 잔액 캐시 저장")
    void getBalance_cacheMiss_loadsFromRepositoryAndCaches() {
        WalletService walletService = new WalletService(
                redisTemplate,
                walletRepository,
                walletTransactionRepository,
                userLockExecutor,
                walletTransactionRecordService
        );

        User user = User.builder()
                .email("wallet-unit@test.com")
                .nickname("wallet_unit")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 10L);
        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.valueOf(12000))
                .lockedBalance(BigDecimal.valueOf(3000))
                .build();

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(redisTemplate.hasKey("user:10:balance")).thenReturn(false);
        when(walletRepository.findByUserId(10L)).thenReturn(Optional.of(wallet));

        walletService.getBalance(10L);

        verify(ops).set(eq("user:10:balance"), eq("12000"), any(Duration.class));
        verify(ops).set(eq("user:10:lockedBalance"), eq("3000"), any(Duration.class));
    }

    @Test
    @DisplayName("지갑 미존재 시 CANNOT_FIND_WALLET 예외 던짐")
    void getBalance_walletNotFound_throwsApiException() {
        WalletService walletService = new WalletService(
                redisTemplate,
                walletRepository,
                walletTransactionRepository,
                userLockExecutor,
                walletTransactionRecordService
        );

        when(redisTemplate.hasKey("user:99:balance")).thenReturn(false);
        when(walletRepository.findByUserId(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> walletService.getBalance(99L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FIND_WALLET);
    }

    @Test
    @DisplayName("지갑 잔액 캐시 키 삭제")
    void clearWalletCache_deletesBothKeys() {
        WalletService walletService = new WalletService(
                redisTemplate,
                walletRepository,
                walletTransactionRepository,
                userLockExecutor,
                walletTransactionRecordService
        );

        walletService.clearWalletCache(33L);

        verify(redisTemplate).delete("user:33:balance");
        verify(redisTemplate).delete("user:33:lockedBalance");
    }

    @Test
    @DisplayName("잔액 부족 시 Redis 증감 롤백 및 INSUFFICIENT_BALANCE 예외 던짐")
    void processBidWallet_insufficientBalance_rollsBackAndThrows() {
        WalletService walletService = new WalletService(
                redisTemplate,
                walletRepository,
                walletTransactionRepository,
                userLockExecutor,
                walletTransactionRecordService
        );

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);

        when(ops.decrement("user:1:balance", 10000L)).thenReturn(-1L);

        ApiException ex = assertThrows(ApiException.class,
                () -> walletService.processBidWallet(1L, -1L, BigDecimal.valueOf(10000), BigDecimal.valueOf(0), 777L, "req-1"));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
        verify(ops).increment("user:1:balance", 10000L);
        verify(ops).decrement("user:1:lockedBalance", 10000L);
    }
}
