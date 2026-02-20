package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserStatus;
import java.time.LocalDateTime;

/**
 * 사용자 차단 응답
 */
@Getter
@AllArgsConstructor
public class AdminBlockUserRes {

    private Long userId;
    private UserStatus status;
    private LocalDateTime blockedAt;
    private String reason;

    public static AdminBlockUserRes from(User user) {
        return new AdminBlockUserRes(
                user.getId(),
                user.getStatus(),
                user.getBlockedAt(),
                user.getBlockReason()
        );
    }
}
