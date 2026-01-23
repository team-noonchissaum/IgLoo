package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 로컬 로그인 응답
 */

@Getter
@AllArgsConstructor
public class TokenRes {

    private String accessToken;

    private String refreshToken;

}
