package noonchissaum.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProfileUpdateUserReq {
    @NotBlank
    private String nickname;
    private String profileUrl;
}
