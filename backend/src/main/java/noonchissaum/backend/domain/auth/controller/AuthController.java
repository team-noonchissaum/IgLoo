package noonchissaum.backend.domain.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auth.dto.request.LoginReq;
import noonchissaum.backend.domain.auth.dto.request.ForgotPasswordReq;
import noonchissaum.backend.domain.auth.dto.request.ResetPasswordReq;
import noonchissaum.backend.domain.auth.dto.request.SignupReq;
import noonchissaum.backend.domain.auth.dto.response.LoginRes;
import noonchissaum.backend.domain.auth.dto.response.RefreshRes;
import noonchissaum.backend.domain.auth.dto.response.SignupRes;
import noonchissaum.backend.domain.auth.service.AuthService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    /**회원 가입*/
    @PostMapping("/signup")
    public ResponseEntity<SignupRes> signup(@Valid @RequestBody SignupReq signupReq) {
        return ResponseEntity.ok(authService.signup(signupReq));
    }

    /**로그인*/
    @PostMapping("/login")
    public ResponseEntity<LoginRes> login(@Valid @RequestBody LoginReq req,
                                          HttpServletResponse response) {
        LoginRes res = authService.login(req);
        addCookie(response,"access_token",res.getAccessToken(),60*30);
        addCookie(response,"refresh_token",res.getRefreshToken(),60*60*24*7);

        return ResponseEntity.ok(res);
    }

    /**토큰 재 발급*/
    @PostMapping("/refresh")
    public ResponseEntity<RefreshRes> refresh(HttpServletRequest request,
                                              HttpServletResponse response) {
        //refresh token 쿠키 꺼내기
        String refreshToken = getCookieValue(request,"refresh_token");
        if(refreshToken==null){
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshRes res = authService.refresh(refreshToken);

        //새 access_token쿠키 갱신
        addCookie(response,"access_token",res.getAccessToken(),60*30);
        addCookie(response, "refresh_token", res.getRefreshToken(), 60 * 60 * 24 * 7);

        return ResponseEntity.ok(res);
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(
            HttpServletRequest request,   // ← 추가
            HttpServletResponse response  // ← 추가
    ) {
        String refreshToken = getCookieValue(request, "refresh_token");
        authService.logout(refreshToken);

        // 쿠키 삭제
        deleteCookie(response, "access_token");
        deleteCookie(response, "refresh_token");

        return ResponseEntity.ok(ApiResponse.success("로그아웃 완료"));
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false)       // 배포 시 true
                .sameSite("Lax")     // 크로스 도메인이면 None 같은 사이트 요청 + 외부에서 링크 클릭으로 들어오는 GET 요청은 허용
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)  // ← 0으로 설정하면 즉시 삭제
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 비밀번호 재설정 요청
     */
    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(@Valid @RequestBody ForgotPasswordReq req) {
        authService.forgotPassword(req.getEmail());
        return ResponseEntity.ok(ApiResponse.success("비밀번호 재설정 요청이 접수되었습니다."));
    }

    /**
     * 비밀번호 재설정
     */
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@Valid @RequestBody ResetPasswordReq req) {
        authService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 재설정되었습니다."));
    }
}
