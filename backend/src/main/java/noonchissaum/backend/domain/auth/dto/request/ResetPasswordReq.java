package noonchissaum.backend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ResetPasswordReq {

    @NotBlank(message = "토큰은 필수 항목입니다.")
    private String token;

    @NotBlank(message = "새 비밀번호는 필수 항목입니다.")
    private String newPassword;
}
