package noonchissaum.backend.domain.user.service;

import noonchissaum.backend.domain.auction.service.AuctionService;
import noonchissaum.backend.domain.user.dto.response.MyPageRes;
import noonchissaum.backend.domain.user.dto.response.UserWalletRes;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private AuctionService auctionService;

    @InjectMocks
    private MyPageService myPageService;

    /**
     * 마이페이지 조회
     */
    @Test
    void getMyPage() {
        // given
        Long userId = 1L;

        Wallet wallet = Wallet.builder()
                .balance(BigDecimal.valueOf(10000))
                .build();

        User user = User.builder()
                .email("test@test.com")
                .nickname("testuser")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "wallet", wallet);

        given(userRepository.findByIdWithWallet(userId))
                .willReturn(Optional.of(user));

        // when
        MyPageRes result = myPageService.getMyPage(userId);

        // then
        assertEquals("testuser", result.getNickname());
        assertEquals(BigDecimal.valueOf(10000), result.getBalance());
    }

    /**
     * 마이페이지 조회 - 유저 없음 예외
     */
    @Test
    void getMyPage_userNotFound_throwsException() {
        // given
        Long userId = 999L;

        given(userRepository.findByIdWithWallet(userId))
                .willReturn(Optional.empty());

        // when & then
        assertThrows(ApiException.class, () ->
                myPageService.getMyPage(userId));
    }

    /**
     * 마이페이지 조회 - 지갑 없음 예외
     */
    @Test
    void getMyPage_walletNotFound_throwsException() {
        // given
        Long userId = 1L;

        User user = User.builder()
                .email("test@test.com")
                .nickname("testuser")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "wallet", null);

        given(userRepository.findByIdWithWallet(userId))
                .willReturn(Optional.of(user));

        // when & then
        assertThrows(ApiException.class, () ->
                myPageService.getMyPage(userId));
    }

    /**
     * 지갑 조회
     */
    @Test
    void getWallet() {
        // given
        Long userId = 1L;

        Wallet wallet = Wallet.builder()
                .balance(BigDecimal.valueOf(10000))
                .lockedBalance(BigDecimal.valueOf(5000))
                .build();

        given(walletRepository.findByUserId(userId))
                .willReturn(Optional.of(wallet));

        // when
        UserWalletRes result = myPageService.getWallet(userId);

        // then
        assertEquals(BigDecimal.valueOf(10000), result.getBalance());
        assertEquals(BigDecimal.valueOf(5000), result.getLockedBalance());
        assertEquals(BigDecimal.valueOf(15000), result.getTotalBalance());
    }

    /**
     * 지갑 조회 - 지갑 없음 예외
     */
    @Test
    void getWallet_notFound_throwsException() {
        // given
        Long userId = 999L;

        given(walletRepository.findByUserId(userId))
                .willReturn(Optional.empty());

        // when & then
        assertThrows(ApiException.class, () ->
                myPageService.getWallet(userId));
    }
}