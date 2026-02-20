package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.user.entity.User;
import java.time.LocalDateTime;

/**
 * 차단된 사용자 목록 조회 응답
 */
@Getter
@AllArgsConstructor
public class AdminBlockedUserRes {
    private Long userId;
    private String email;
    private String nickname;
    private LocalDateTime blockedAt;
    private String blockReason;

    public static AdminBlockedUserRes from(User user) {
        return new AdminBlockedUserRes(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getBlockedAt(),
                user.getBlockReason()
        );
    }
}
