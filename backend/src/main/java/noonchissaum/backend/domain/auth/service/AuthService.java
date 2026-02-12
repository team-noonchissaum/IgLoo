package noonchissaum.backend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auth.dto.request.LoginReq;
import noonchissaum.backend.domain.auth.dto.request.RefreshReq;
import noonchissaum.backend.domain.auth.dto.request.SignupReq;
import noonchissaum.backend.domain.auth.dto.response.LoginRes;
import noonchissaum.backend.domain.auth.dto.response.RefreshRes;
import noonchissaum.backend.domain.auth.dto.response.SignupRes;
import noonchissaum.backend.domain.auth.entity.AuthType;
import noonchissaum.backend.domain.auth.entity.UserAuth;
import noonchissaum.backend.domain.auth.repository.UserAuthRepository;
import noonchissaum.backend.domain.user.entity.*;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.security.JwtTokenProvider;
import noonchissaum.backend.global.service.MailService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final MailService mailService;
    private final WalletService walletService;

    /**로컬 회원가입*/
    @Transactional
    public SignupRes signup(SignupReq signupReq) {
        if(userRepository.existsByEmailAndNotDeleted(signupReq.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        if(userRepository.existsByNicknameAndNotDeleted(signupReq.getNickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }
        User user= new User(
                signupReq.getEmail(),
                signupReq.getNickname(),
                UserRole.USER,
                UserStatus.ACTIVE
        );
        User saved = userRepository.save(user);

        UserAuth userAuth = UserAuth.createLocal(
                user,
                signupReq.getEmail(),
                passwordEncoder.encode(signupReq.getPassword())
        );
        userAuthRepository.save(userAuth);

        Wallet wallet = walletService.createWallet(saved);
        saved.registerWallet(wallet);

        return new SignupRes(
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );
    }

    /**
     * 로그인 처리
     * */
    public LoginRes login(LoginReq req) {

        UserAuth userAuth;
        boolean isNewer = false;

        if (req.getAuthType() == AuthType.LOCAL) {
            userAuth = localLogin(req);
        } else {
            LoginResult result = oauthLogin(req);
            userAuth = result.userAuth();
            isNewer = result.isNewer();
        }

        User user = userAuth.getUser();

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(),user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        //Redis에 Refresh토큰을 저장
        refreshTokenService.save(
                user.getId(),
                refreshToken,
                60L*60*24*7
        );

        return new LoginRes(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                accessToken,
                refreshToken,
                isNewer
        );
    }

    /**
     * Local로그인
     * */
    private UserAuth localLogin(LoginReq req) {
        UserAuth userAuth = userAuthRepository
                .findByAuthTypeAndIdentifier(AuthType.LOCAL,req.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_LOGIN));

        if (!passwordEncoder.matches(req.getPassword(), userAuth.getPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_LOGIN);
        }

        return userAuth;
    }

    /**
     * OAuth로그인
     * */
    private LoginResult oauthLogin(LoginReq req) {

        // 🔥 실제로는 provider별로 토큰 검증 필요
        String oauthIdentifier = req.getOauthToken(); // 예시용

        return userAuthRepository
                .findByAuthTypeAndIdentifier(req.getAuthType(),oauthIdentifier)
                .map(auth -> new LoginResult(auth, false))
                .orElseGet(() -> oauthSignup(req, oauthIdentifier));
    }

    /**
     * OAuth 신규 회원가입
     * */
    @Transactional
    protected LoginResult oauthSignup(LoginReq req, String identifier) {

        if (userAuthRepository.existsByIdentifierAndAuthType(req.getAuthType(),identifier)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (req.getNickname() == null || req.getNickname().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        User user = new User(
                req.getEmail(),
                req.getNickname(),
                UserRole.USER,
                UserStatus.ACTIVE
        );

        User saved = userRepository.save(user);

        UserAuth userAuth = UserAuth.oauth(user, req.getAuthType(), identifier);
        userAuthRepository.save(userAuth);

        Wallet wallet = walletService.createWallet(saved);
        saved.registerWallet(wallet);

        return new LoginResult(userAuth, true);
    }

    /**
     * 토큰 재발급(refresh)
     * */
    public RefreshRes refresh(RefreshReq req) {
        String refreshToken= req.getRefreshToken();

        if(!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        Long userId=jwtTokenProvider.getUserId(refreshToken);

        //Redis에 저장된 RT와 비교+ 중복로그인/재사용 방지
        if(!refreshTokenService.isValid(userId, refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));
        //기존 RT제거(rotation)
        refreshTokenService.delete(userId);

        //새 토큰 발급
        String newAccessToken=
                jwtTokenProvider.createAccessToken(user.getId(),user.getRole());
        String newRefreshToken=
                jwtTokenProvider.createRefreshToken(user.getId());

        //새 RT저장
        refreshTokenService.save(
                user.getId(),
                newRefreshToken,
                60L*60*24*7
        );
        return new RefreshRes(newAccessToken,newRefreshToken);
    }

    /**
     * 비밀번호 재설정 요청
     * */
    public void forgotPassword(String email) {
        UserAuth userAuth = userAuthRepository
                .findByAuthTypeAndIdentifier(AuthType.LOCAL, email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        String token = passwordResetTokenService.createToken(userAuth.getUser().getId());
        mailService.sendPasswordResetMail(email, token);
    }

    /**
     * 비밀번호 재설정
     * */
    public void resetPassword(String token, String newPassword) {
        Long userId = passwordResetTokenService.getUserIdByToken(token);
        if (userId == null) {
            throw new ApiException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        UserAuth userAuth = userAuthRepository
                .findByUser_IdAndAuthType(userId, AuthType.LOCAL)
                .orElseThrow(() -> new ApiException(ErrorCode.PASSWORD_RESET_LOCAL_ONLY));

        userAuth.changePassword(passwordEncoder.encode(newPassword));

        // 비밀번호 변경 후 기존 로그인 세션 무효화
        refreshTokenService.delete(userId);
        passwordResetTokenService.deleteToken(token);
    }

    /**
     * 로그아웃
     * */
    public void logout(String refreshToken) {
        if(!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId=jwtTokenProvider.getUserId(refreshToken);
        refreshTokenService.delete(userId);
    }

    /**
     * OAuth 로그인 결과용 내부 record
     * */
    private record LoginResult(UserAuth userAuth, boolean isNewer) {
    }
}
