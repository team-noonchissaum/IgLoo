package noonchissaum.backend.global.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserStatus;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SecurityUserCache {
    private Long id;
    private String email;
    private String role;
    private UserStatus status;

    public SecurityUserCache(User user){
        this.id = user.getId();
        this.email = user.getEmail();
        this.role = user.getRole().name();
        this.status = user.getStatus();
    }
}
