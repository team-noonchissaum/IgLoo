package noonchissaum.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ProfileUpdateUserReq {

    @NotBlank
    private String nickname;


    private String profileUrl;

}
