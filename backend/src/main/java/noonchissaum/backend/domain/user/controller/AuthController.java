package noonchissaum.backend.domain.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.user.dto.request.LocalsignupReq;
import noonchissaum.backend.domain.user.dto.request.LoginReq;
import noonchissaum.backend.domain.user.dto.response.SignupRes;
import noonchissaum.backend.domain.user.dto.response.TokenRes;
import noonchissaum.backend.domain.user.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SignupRes> signup(
            @Valid @RequestBody LocalsignupReq request
    ) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenRes> login(
            @Valid @RequestBody LoginReq request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }
}
