package noonchissaum.backend.domain.auth.oauth2.userinfo;

import java.util.Map;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> new GoogleUserInfo(attributes);
            case "kakao" -> new KakaoUserInfo(attributes);
            case "naver" -> new NaverUserInfo(attributes);
            default -> throw new ApiException(ErrorCode.OAUTH_UNSUPPORTED_PROVIDER);
        };
    }
}
