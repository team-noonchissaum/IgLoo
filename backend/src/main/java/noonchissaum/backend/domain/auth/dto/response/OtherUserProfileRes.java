package noonchissaum.backend.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OtherUserProfileRes {

    private Long userId;
    private String nickname;
    private String profileUrl;
    //private String location;
}
