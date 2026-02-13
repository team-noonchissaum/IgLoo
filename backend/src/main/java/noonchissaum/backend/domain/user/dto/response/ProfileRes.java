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
    private String dong;

    /** 차단 시에만 포함 */
    private String blockReason;

    public static ProfileRes of(Long userId, String nickname, String profileUrl, String email,
                                String role, String status, String blockReason, String dong) {
        return new ProfileRes(userId, nickname, profileUrl, email, role, status, blockReason, dong);
    }
}
