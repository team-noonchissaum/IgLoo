package noonchissaum.backend.domain.auth.oauth2.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {

        response.setContentType("application/json;charset=UTF-8");

        ErrorCode errorCode;

        // 차단된 사용자인 경우
        if (exception instanceof OAuth2AuthenticationException oauthEx
                && "user_blocked".equals(oauthEx.getError().getErrorCode())) {
            errorCode = ErrorCode.USER_BLOCKED;
        } else {
            errorCode = ErrorCode.OAUTH2_LOGIN_FAILED;
        }

        response.setStatus(errorCode.getStatus().value());

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("code", errorCode.getCode());
        body.put("message", errorCode.getMessage());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
