package noonchissaum.backend.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupRes {
    private Long userId;
    private String email;
    private String nickname;
}
