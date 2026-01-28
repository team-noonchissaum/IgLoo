package noonchissaum.backend.domain.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LogoutReq {
    private String refreshToken;
}
