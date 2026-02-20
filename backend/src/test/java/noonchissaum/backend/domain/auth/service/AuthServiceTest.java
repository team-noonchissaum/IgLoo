package noonchissaum.backend.domain.auth.service;

import noonchissaum.backend.domain.auth.dto.request.LoginReq;
import noonchissaum.backend.domain.auth.dto.request.RefreshReq;
import noonchissaum.backend.domain.auth.dto.request.SignupReq;
import noonchissaum.backend.domain.auth.dto.response.LoginRes;
import noonchissaum.backend.domain.auth.dto.response.RefreshRes;
import noonchissaum.backend.domain.auth.dto.response.SignupRes;
import noonchissaum.backend.domain.auth.entity.AuthType;
import noonchissaum.backend.domain.auth.entity.UserAuth;
import noonchissaum.backend.domain.auth.repository.UserAuthRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.user.service.UserLocationService;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.security.JwtTokenProvider;
import noonchissaum.backend.global.service.MailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock private UserAuthRepository userAuthRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private PasswordResetTokenService passwordResetTokenService;
    @Mock private MailService mailService;
    @Mock private WalletService walletService;
    @Mock private UserLocationService userLocationService;

    @InjectMocks
    private AuthService authService;

    /* ================= 회원가입 ================= */

    /**
     * 회원가입
     */
    @Test
    void signup() {
        // given
        SignupReq req = new SignupReq();
        ReflectionTestUtils.setField(req, "email", "test@test.com");
        ReflectionTestUtils.setField(req, "password", "password123");
        ReflectionTestUtils.setField(req, "nickname", "testuser");
        ReflectionTestUtils.setField(req, "address", "서울 강남구 테헤란로 111");

        User savedUser = User.builder()
                .email("test@test.com")
                .nickname("testuser")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(savedUser, "id", 1L);

        Wallet wallet = Wallet.builder()
                .balance(BigDecimal.ZERO)
                .build();

        given(userRepository.existsByEmailAndNotDeleted("test@test.com"))
                .willReturn(false);
        given(userRepository.existsByNicknameAndNotDeleted("testuser"))
                .willReturn(false);
        given(userRepository.save(any(User.class)))
                .willReturn(savedUser);
        given(passwordEncoder.encode("password123"))
                .willReturn("encodedPassword");
        given(walletService.createWallet(any(User.class)))
                .willReturn(wallet);

        // when
        SignupRes result = authService.signup(req);

        // then
        assertEquals("test@test.com", result.getEmail());
        assertEquals("testuser", result.getNickname());
        verify(userLocationService, times(1)).updateLocation(anyLong(), any());
    }

    /**
     * 회원가입 - 이메일 중복 예외
     */
    @Test
    void signup_duplicateEmail_throwsException() {
        // given
        SignupReq req = new SignupReq();
        ReflectionTestUtils.setField(req, "email", "duplicate@test.com");
        ReflectionTestUtils.setField(req, "password", "password123");
        ReflectionTestUtils.setField(req, "nickname", "testuser");
        ReflectionTestUtils.setField(req, "address", "서울 강남구");

        given(userRepository.existsByEmailAndNotDeleted("duplicate@test.com"))
                .willReturn(true);

        // when & then
        assertThrows(ApiException.class, () ->
                authService.signup(req));
    }

    /**
     * 회원가입 - 닉네임 중복 예외
     */
    @Test
    void signup_duplicateNickname_throwsException() {
        // given
        SignupReq req = new SignupReq();
        ReflectionTestUtils.setField(req, "email", "test@test.com");
        ReflectionTestUtils.setField(req, "password", "password123");
        ReflectionTestUtils.setField(req, "nickname", "duplicateNick");
        ReflectionTestUtils.setField(req, "address", "서울 강남구");

        given(userRepository.existsByEmailAndNotDeleted("test@test.com"))
                .willReturn(false);
        given(userRepository.existsByNicknameAndNotDeleted("duplicateNick"))
                .willReturn(true);

        // when & then
        assertThrows(ApiException.class, () ->
                authService.signup(req));
    }

    /* ================= 로그인 ================= */

    /**
     * 로컬 로그인
     */
    @Test
    void login_local() {
        // given
        LoginReq req = new LoginReq();
        ReflectionTestUtils.setField(req, "authType", AuthType.LOCAL);
        ReflectionTestUtils.setField(req, "email", "test@test.com");
        ReflectionTestUtils.setField(req, "password", "password123");

        User user = User.builder()
                .email("test@test.com")
                .nickname("testuser")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        UserAuth userAuth = UserAuth.createLocal(user, "test@test.com", "encodedPassword");

        given(userAuthRepository.findByAuthTypeAndIdentifier(AuthType.LOCAL, "test@test.com"))
                .willReturn(Optional.of(userAuth));
        given(passwordEncoder.matches("password123", "encodedPassword"))
                .willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L, UserRole.USER))
                .willReturn("accessToken");
        given(jwtTokenProvider.createRefreshToken(1L))
                .willReturn("refreshToken");

        // when
        LoginRes result = authService.login(req);

        // then
        assertEquals("test@test.com", result.getEmail());
        assertEquals("accessToken", result.getAccessToken());
        verify(refreshTokenService, times(1)).save(anyLong(), anyString(), anyLong());
    }

    /**
     * 로컬 로그인 - 유저 없음 예외
     */
    @Test
    void login_local_userNotFound_throwsException() {
        // given
        LoginReq req = new LoginReq();
        ReflectionTestUtils.setField(req, "authType", AuthType.LOCAL);
        ReflectionTestUtils.setField(req, "email", "notfound@test.com");
        ReflectionTestUtils.setField(req, "password", "password123");

        given(userAuthRepository.findByAuthTypeAndIdentifier(AuthType.LOCAL, "notfound@test.com"))
                .willReturn(Optional.empty());

        // when & then
        assertThrows(ApiException.class, () ->
                authService.login(req));
    }

    /**
     * 로컬 로그인 - 비밀번호 틀림 예외
     */
    @Test
    void login_local_wrongPassword_throwsException() {
        // given
        LoginReq req = new LoginReq();
        ReflectionTestUtils.setField(req, "authType", AuthType.LOCAL);
        ReflectionTestUtils.setField(req, "email", "test@test.com");
        ReflectionTestUtils.setField(req, "password", "wrongPassword");

        User user = User.builder()
                .email("test@test.com")
                .nickname("testuser")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        UserAuth userAuth = UserAuth.createLocal(user, "test@test.com", "encodedPassword");

        given(userAuthRepository.findByAuthTypeAndIdentifier(AuthType.LOCAL, "test@test.com"))
                .willReturn(Optional.of(userAuth));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword"))
                .willReturn(false);

        // when & then
        assertThrows(ApiException.class, () ->
                authService.login(req));
    }

    /* ================= 토큰 재발급 ================= */

    /**
     * 토큰 재발급
     */
    @Test
    void refresh() {
        // given
        RefreshReq req = new RefreshReq();
        ReflectionTestUtils.setField(req, "refreshToken", "validRefreshToken");

        User user = User.builder()
                .email("test@test.com")
                .nickname("testuser")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        given(jwtTokenProvider.validateToken("validRefreshToken"))
                .willReturn(true);
        given(jwtTokenProvider.getUserId("validRefreshToken"))
                .willReturn(1L);
        given(refreshTokenService.isValid(1L, "validRefreshToken"))
                .willReturn(true);
        given(userRepository.findById(1L))
                .willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(1L, UserRole.USER))
                .willReturn("newAccessToken");
        given(jwtTokenProvider.createRefreshToken(1L))
                .willReturn("newRefreshToken");

        // when
        RefreshRes result = authService.refresh(req);

        // then
        assertEquals("newAccessToken", result.getAccessToken());
        assertEquals("newRefreshToken", result.getRefreshToken());
        verify(refreshTokenService, times(1)).delete(1L);
    }

    /**
     * 토큰 재발급 - 유효하지 않은 토큰 예외
     */
    @Test
    void refresh_invalidToken_throwsException() {
        // given
        RefreshReq req = new RefreshReq();
        ReflectionTestUtils.setField(req, "refreshToken", "invalidToken");

        given(jwtTokenProvider.validateToken("invalidToken"))
                .willReturn(false);

        // when & then
        assertThrows(ApiException.class, () ->
                authService.refresh(req));
    }

    /* ================= 로그아웃 ================= */

    /**
     * 로그아웃
     */
    @Test
    void logout() {
        // given
        String refreshToken = "validRefreshToken";

        given(jwtTokenProvider.validateToken(refreshToken))
                .willReturn(true);
        given(jwtTokenProvider.getUserId(refreshToken))
                .willReturn(1L);

        // when
        authService.logout(refreshToken);

        // then
        verify(refreshTokenService, times(1)).delete(1L);
    }

    /**
     * 로그아웃 - 유효하지 않은 토큰 예외
     */
    @Test
    void logout_invalidToken_throwsException() {
        // given
        String refreshToken = "invalidToken";

        given(jwtTokenProvider.validateToken(refreshToken))
                .willReturn(false);

        // when & then
        assertThrows(ApiException.class, () ->
                authService.logout(refreshToken));
    }

    /* ================= 비밀번호 재설정 ================= */

    /**
     * 비밀번호 재설정 요청
     */
    @Test
    void forgotPassword() {
        // given
        String email = "test@test.com";

        User user = User.builder()
                .email(email)
                .nickname("testuser")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        UserAuth userAuth = UserAuth.createLocal(user, email, "encodedPassword");

        given(userAuthRepository.findByAuthTypeAndIdentifier(AuthType.LOCAL, email))
                .willReturn(Optional.of(userAuth));
        given(passwordResetTokenService.createToken(1L))
                .willReturn("resetToken");

        // when
        authService.forgotPassword(email);

        // then
        verify(mailService, times(1)).sendPasswordResetMail(email, "resetToken");
    }

    /**
     * 비밀번호 재설정
     */
    @Test
    void resetPassword() {
        // given
        String token = "validResetToken";
        String newPassword = "newPassword123";

        User user = User.builder()
                .email("test@test.com")
                .nickname("testuser")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        UserAuth userAuth = UserAuth.createLocal(user, "test@test.com", "oldEncodedPassword");

        given(passwordResetTokenService.getUserIdByToken(token))
                .willReturn(1L);
        given(userAuthRepository.findByUser_IdAndAuthType(1L, AuthType.LOCAL))
                .willReturn(Optional.of(userAuth));
        given(passwordEncoder.encode(newPassword))
                .willReturn("newEncodedPassword");

        // when
        authService.resetPassword(token, newPassword);

        // then
        verify(refreshTokenService, times(1)).delete(1L);
        verify(passwordResetTokenService, times(1)).deleteToken(token);
    }

    /**
     * 비밀번호 재설정 - 유효하지 않은 토큰 예외
     */
    @Test
    void resetPassword_invalidToken_throwsException() {
        // given
        String token = "invalidToken";
        String newPassword = "newPassword123";

        given(passwordResetTokenService.getUserIdByToken(token))
                .willReturn(null);

        // when & then
        assertThrows(ApiException.class, () ->
                authService.resetPassword(token, newPassword));
    }
}