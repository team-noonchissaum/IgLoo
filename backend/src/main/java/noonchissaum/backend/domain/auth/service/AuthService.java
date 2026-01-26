package noonchissaum.backend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auth.entity.AuthType;
import noonchissaum.backend.domain.auth.entity.UserAuth;
import noonchissaum.backend.domain.auth.dto.response.SignupRes;
import noonchissaum.backend.domain.auth.dto.response.TokenRes;
import noonchissaum.backend.domain.user.entity.*;
import noonchissaum.backend.domain.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupRes signup(LocalsignupReq request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
        }

        User user= new User(
                request.getEmail(),
                request.getNickname(),
                UserRole.USER,
                UserStatus.ACTIVE
        );


        UserAuth auth = new UserAuth(
                user,
                AuthType.LOCAL,
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        user.getAuths().add(auth);
        userRepository.save(user);

        return new SignupRes(
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );

    }
    public TokenRes login(OAuthLoginReq request) {
        User user = userRepository.findByEmailWithAuths(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자"));

        // ACTIVE 상태 유저인지 검증 - BLOCKED된 유저는 로그인 불가능
        if (!user.isActive()) {
            throw new IllegalArgumentException("차단되었거나 탈퇴한 계정입니다.");
        }

        // 비밀번호 검증
        UserAuth auth = user.getAuths().stream()
                .filter(a -> a.getAuthType() == AuthType.LOCAL)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("로컬 로그인 불가"));

        if (!passwordEncoder.matches(request.getPassword(), auth.getPasswordHash())) {
            throw new IllegalArgumentException("비밀번호 불일치");
        }

        return new TokenRes(
                "ACCESS_TOKEN_SAMPLE",
                "REFRESH_TOKEN_SAMPLE"
        );
    }
}
