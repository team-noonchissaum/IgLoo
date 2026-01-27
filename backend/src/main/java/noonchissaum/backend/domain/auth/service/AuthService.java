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
import noonchissaum.backend.global.config.JwtTokenProvider;
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

    /**로컬 회원가입*/
    public SignupRes signup(SignupReq signupReq) {
        if(userRepository.existsByEmail(signupReq.getEmail())) {
            throw new IllegalArgumentException("이미 사용중인 이메일 입니다");
        }
        if(userRepository.existsByNickname(signupReq.getNickname())) {
            throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
        }
        User user= new User(
                signupReq.getEmail(),
                signupReq.getNickname(),
                UserRole.USER,
                UserStatus.ACTIVE
        );
        userRepository.save(user);

        UserAuth userAuth = UserAuth.createLocal(
                user,
                signupReq.getEmail(),
                passwordEncoder.encode(signupReq.getPassword())
        );
        userAuthRepository.save(userAuth);

        return new SignupRes(
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );
    }

    /**로그인 처리*/
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
     */
    private UserAuth localLogin(LoginReq req) {
        UserAuth userAuth = userAuthRepository
                .findByAuthTypeAndIdentifier(AuthType.LOCAL , req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계정입니다."));

        if (!passwordEncoder.matches(req.getPassword(), userAuth.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return userAuth;
    }

    /**
     * OAuth로그인
     */
    private LoginResult oauthLogin(LoginReq req) {

        // 🔥 실제로는 provider별로 토큰 검증 필요
        String oauthIdentifier = req.getOauthToken(); // 예시용

        return userAuthRepository
                .findByAuthTypeAndIdentifier(req.getAuthType() , oauthIdentifier)
                .map(auth -> new LoginResult(auth, false))
                .orElseGet(() -> oauthSignup(req, oauthIdentifier));
    }

    /**
     * OAuth 신규 회원가입
     */
    private LoginResult oauthSignup(LoginReq req, String identifier) {

        if (userAuthRepository.existsByIdentifierAndAuthType(req.getAuthType(),identifier)) {
            throw new IllegalArgumentException("이미 가입된 OAuth 계정입니다.");
        }

        if (req.getNickname() == null || req.getNickname().isBlank()) {
            throw new IllegalArgumentException("신규 OAuth 회원은 닉네임이 필요합니다.");
        }

        User user = new User(
                req.getEmail(),
                req.getNickname(),
                UserRole.USER,
                UserStatus.ACTIVE
        );

        userRepository.save(user);

        UserAuth userAuth = UserAuth.oauth(user, req.getAuthType(), identifier);
        userAuthRepository.save(userAuth);

        return new LoginResult(userAuth, true);
    }

    /**토큰 재발급(refresh)*/
    public RefreshRes refresh(RefreshReq req) {
        Long userId =jwtTokenProvider.getUserId(req.getRefreshToken());

        User user = userRepository.findById(userId).orElseThrow(()-> new IllegalArgumentException("유저가 존재하지 않음"));

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(),user.getRole());

        return new RefreshRes(newAccessToken);
    }

    /**로그아웃*/
    public void logout(String refreshToken) {
        //추후 redis도입해서 삭제 처리 할 부분.
    }



    /**
     * OAuth 로그인 결과용 내부 record
     */
    private record LoginResult(UserAuth userAuth, boolean isNewer) {
    }
}
