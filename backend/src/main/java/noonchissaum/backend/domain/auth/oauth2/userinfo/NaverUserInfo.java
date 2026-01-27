package noonchissaum.backend.domain.auth.oauth2.userinfo;

import java.util.Map;

@SuppressWarnings("unchecked")
public class NaverUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> response;

    public NaverUserInfo(Map<String, Object> attributes) {
        this.response = (Map<String, Object>) attributes.get("response");
    }

    @Override public String getProviderId() { return response == null ? null : (String) response.get("id"); }
    @Override public String getEmail() { return response == null ? null : (String) response.get("email"); }
    @Override public String getNickname() { return response == null ? "naver_user" : (String) response.getOrDefault("name", "naver_user"); }
    @Override public String getProfileUrl() { return response == null ? null : (String) response.get("profile_image"); }
}
