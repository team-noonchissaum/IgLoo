package noonchissaum.backend.domain.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auth.dto.request.LoginReq;
import noonchissaum.backend.domain.auth.dto.request.LogoutReq;
import noonchissaum.backend.domain.auth.dto.request.ForgotPasswordReq;
import noonchissaum.backend.domain.auth.dto.request.RefreshReq;
import noonchissaum.backend.domain.auth.dto.request.ResetPasswordReq;
import noonchissaum.backend.domain.auth.dto.request.SignupReq;
import noonchissaum.backend.domain.auth.dto.response.LoginRes;
import noonchissaum.backend.domain.auth.dto.response.RefreshRes;
import noonchissaum.backend.domain.auth.dto.response.SignupRes;
import noonchissaum.backend.domain.auth.service.AuthService;
import noonchissaum.backend.global.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<LoginRes> login(@Valid @RequestBody LoginReq req) {
        LoginRes res = authService.login(req);
        return ResponseEntity.ok(res);
    }

    /**토큰 재 발급*/
    @PostMapping("/refresh")
    public ResponseEntity<RefreshRes> refresh(@Valid @RequestBody RefreshReq req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(@RequestBody LogoutReq req) {
        authService.logout(req.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("로그아웃 완료"));
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
