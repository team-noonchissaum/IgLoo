package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuthLoginRes {
    private String accessToken;
    private String refreshToken;
    private boolean isNewUser;
}
