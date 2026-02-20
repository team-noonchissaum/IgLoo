package noonchissaum.backend.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.user.entity.UserRole;

@Getter
@AllArgsConstructor
public class LoginRes {
    private Long userId;
    private String email;
    private String nickname;
    private UserRole role;
    private String accessToken;
    private String refreshToken;
    private boolean isNewer;
}
