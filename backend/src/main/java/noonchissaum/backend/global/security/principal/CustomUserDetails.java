package noonchissaum.backend.global.security.principal;

import noonchissaum.backend.domain.auth.entity.UserAuth;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails, AuthenticatedUser {
    private final User user;
    private final UserAuth userAuth;

    //로그인(Local,OAuth) 시점
    public CustomUserDetails(User user,UserAuth userAuth) {
        this.user = user;
        this.userAuth = userAuth;
    }

    //jwt 인증 시점
    public CustomUserDetails(User user) {
        this.user = user;
        this.userAuth = null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("Role: "+ user.getRole().name())
        );
    }

    @Override
    public Long getUserId() {
        return user.getId();
    }

    @Override
    public UserRole getRole() {
        return user.getRole();
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }
    @Override
    public String getPassword() {
        return userAuth != null ? userAuth.getPasswordHash() : null;
    }

    @Override
    public String getUsername() {
        return user.getEmail();
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
        return user.getStatus() == UserStatus.ACTIVE;
    }


}
