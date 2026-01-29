package noonchissaum.backend.domain.auth.oauth2.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auth.entity.UserAuth;
import noonchissaum.backend.domain.auth.entity.AuthType;
import noonchissaum.backend.domain.auth.repository.UserAuthRepository;
import noonchissaum.backend.domain.auth.oauth2.userinfo.OAuth2UserInfo;
import noonchissaum.backend.domain.auth.oauth2.userinfo.OAuth2UserInfoFactory;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final WalletService walletService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // google/kakao/naver
        AuthType authType = AuthType.valueOf(registrationId.toUpperCase());

        OAuth2UserInfo info = OAuth2UserInfoFactory.of(registrationId, oAuth2User.getAttributes());

        String providerId = info.getProviderId();
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId is null/blank");
        }

        // identifier 규칙: {AUTH_TYPE}:{providerId}
        String identifier = authType.name() + ":" + providerId;

        // 1) UserAuth로 기존 가입 여부 확인
        User user = userAuthRepository.findByAuthTypeAndIdentifier(authType, identifier)
                .map(UserAuth::getUser)
                .orElseGet(() -> createNewOAuthUser(authType, identifier, info));

        // 차단된 사용자 체크
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_blocked", "차단된 사용자입니다. 관리자에게 문의하세요.", null)
            );
        }

        return UserPrincipal.from(user, oAuth2User.getAttributes());
    }

    private User createNewOAuthUser(AuthType authType, String identifier, OAuth2UserInfo info) {
        // email이 없을 수도 있으니 임시 이메일 생성
        String email = info.getEmail();
        if (email == null || email.isBlank()) {
            String safe = identifier.replace(":", "_");
            email = safe + "@oauth.local";
        }

        String nickname = info.getNickname();
        if (nickname == null || nickname.isBlank()) nickname = authType.name().toLowerCase() + "_user";

        // 닉네임 중복 방지(간단히 suffix)
        if (userRepository.existsByNickname(nickname)) {
            nickname = nickname + "_" + UUID.randomUUID().toString().substring(0, 8);
        }

        User newUser = new User(email, nickname, UserRole.USER, UserStatus.ACTIVE);
        // 프로필 URL 있으면 저장 (null이면 그대로)
        newUser.updateProfile(nickname, info.getProfileUrl());

        User saved = userRepository.save(newUser);

        UserAuth oauthAuth = UserAuth.oauth(newUser, authType, identifier);
        userAuthRepository.save(oauthAuth);

        Wallet wallet = walletService.createWallet(saved.getId());
        saved.registWallet(wallet);

        return newUser;
    }
}