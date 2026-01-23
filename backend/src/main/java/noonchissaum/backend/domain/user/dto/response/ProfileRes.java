package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class ProfileRes {
    private Long userId;
    private String nickname;
    private String profileUrl;
    private String email;
    private String role;
    private String status;

}
