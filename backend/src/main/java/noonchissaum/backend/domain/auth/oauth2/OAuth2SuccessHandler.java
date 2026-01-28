package noonchissaum.backend.domain.auth.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.global.config.JwtTokenProvider;
import noonchissaum.backend.global.security.principal.CustomOAuth2User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.oauth2.redirect-url:http://localhost:3000/oauth/callback}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();


        String accessToken = jwtTokenProvider.createAccessToken(principal.getUserId(), principal.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(principal.getUserId());

        //  시연 방식: redirect query로 전달
        // 프론트에서 token 저장 후 Authorization: Bearer 로 호출
        response.sendRedirect(redirectUrl
                + "?accessToken=" + accessToken
                + "&refreshToken=" + refreshToken
        );
    }
}