package noonchissaum.backend.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RefreshRes {
    private String accessToken;
    private String refreshToken;
}
