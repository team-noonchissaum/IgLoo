package noonchissaum.backend.global.security;

import lombok.Getter;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * 통합 Principal
 * 로컬, 소셜 @AuthenticationPrincipal로 통일해서 사용
 */

@Getter
public class UserPrincipal implements UserDetails, OAuth2User {

    private final Long userId;
    private final String email;
    private final String password;
    private final UserRole role;
    private final Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;  // OAuth2용

    // 로컬 로그인
    public UserPrincipal(Long userId, String email, String password, UserRole role) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.role = role;
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    // OAuth2 로그인
    public UserPrincipal(Long userId, String email, UserRole role, Map<String, Object> attributes) {
        this.userId = userId;
        this.email = email;
        this.password = null;  // OAuth2는 비밀번호 없음
        this.role = role;
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
        this.attributes = attributes;
    }

    // 로컬용
    public static UserPrincipal from(User user, String password) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                password,
                user.getRole()
        );
    }

    // OAuth2용
    public static UserPrincipal from(User user, Map<String, Object> attributes) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                attributes
        );
    }

    // JWT 토큰에서 생성 (필터용)
    public static UserPrincipal of(Long userId, UserRole role) {
        return new UserPrincipal(userId, null, null, role);
    }

    // ========== UserDetails 메서드 ==========

    @Override
    public String getUsername() {
        return String.valueOf(userId);  // userId를 username으로 사용
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // ========== OAuth2User 메서드 ==========

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}

