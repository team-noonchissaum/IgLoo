package noonchissaum.backend.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/oauth2/login")
@RequiredArgsConstructor
public class OAuth2LoginController {

    @GetMapping("/{provider}")
    public void oauth2Login(@PathVariable String provider,
                            HttpServletResponse response
    )throws IOException {
        //provider검증
        if (!List.of("google", "kakao", "naver").contains(provider)) {
            throw new ApiException(ErrorCode.OAUTH_UNSUPPORTED_PROVIDER);
        }
        //외부 OAuth와 다른 내부 OAuth엔드포인트 redirect
        response.sendRedirect("/oauth2/authorization/"+provider);
    }
}
