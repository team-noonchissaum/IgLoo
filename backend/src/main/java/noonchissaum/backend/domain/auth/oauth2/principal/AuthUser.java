package noonchissaum.backend.domain.auth.oauth2.principal;

import noonchissaum.backend.domain.user.entity.UserRole;

public interface AuthUser {
    Long getUserId();
    UserRole getRole();
    String getEmail();
}
