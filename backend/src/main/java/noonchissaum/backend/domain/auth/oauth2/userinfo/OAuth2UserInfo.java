package noonchissaum.backend.domain.auth.oauth2.userinfo;

public interface OAuth2UserInfo {
    String getProviderId();
    String getEmail();       // 동의 안 하면 null 가능
    String getNickname();
    String getProfileUrl();
}
