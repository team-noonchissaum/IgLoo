package noonchissaum.backend.global.security;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auth.entity.AuthType;
import noonchissaum.backend.domain.auth.entity.UserAuth;
import noonchissaum.backend.domain.auth.repository.UserAuthRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 로컬 로그인용
 * security가 로컬 인증 시 사용
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAuthRepository userAuthRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAuth userAuth = userAuthRepository
                .findByAuthTypeAndIdentifier(AuthType.LOCAL, email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        User user = userAuth.getUser();

        if(user.getStatus()== UserStatus.BLOCKED){
            throw new CustomException(ErrorCode.USER_BLOCKED);
        }
        return UserPrincipal.from(user, userAuth.getPasswordHash());
    }

    // userId로 조회 (JWT 필터용)
    public UserPrincipal loadUserById(Long userId) {
        // JWT에 role 정보가 있으므로 간단히 처리
        return null;  // JwtAuthenticationFilter에서 직접 생성해주기!
    }
}
