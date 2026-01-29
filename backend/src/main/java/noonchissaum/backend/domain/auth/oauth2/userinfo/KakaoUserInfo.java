package noonchissaum.backend.domain.auth.oauth2.userinfo;

import java.util.Map;

@SuppressWarnings("unchecked")
public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        return account == null ? null : (String) account.get("email");
    }

    @Override
    public String getNickname() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account == null) return "kakao_user";
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");
        if (profile == null) return "kakao_user";
        return (String) profile.getOrDefault("nickname", "kakao_user");
    }

    @Override
    public String getProfileUrl() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account == null) return null;
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");
        if (profile == null) return null;
        return (String) profile.get("profile_image_url");
    }
}