package noonchissaum.backend.global.security.principal;

import noonchissaum.backend.domain.user.entity.UserRole;

public interface AuthenticatedUser {
    Long getUserId();
    UserRole getRole();
    String getEmail();
}
