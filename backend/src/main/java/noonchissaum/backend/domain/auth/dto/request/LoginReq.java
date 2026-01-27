package noonchissaum.backend.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.auth.entity.AuthType;

/**
 * 회원가입 요청 DTO
 * 로컬 회원가입시 이메일
 */

@Getter
@NoArgsConstructor
public class LoginReq {

    @NotNull
    private AuthType authType;// LOCAL,GOOGLE,KAKAO,NAVER

    @NotBlank(message = "이메일은 필수 항목입니다.")
    @Email(message = "올바르지 않은 이메일 형식입니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 항목입니다.")
    private String password;

    private String nickname;//OAuth신규유저일때만 사용,Local에서는 무시

    private String oauthToken;
}
