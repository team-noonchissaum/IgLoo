package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;

import java.time.LocalDateTime;

/**
 * 사용자 목록 조회 응답
 */

@Getter
@AllArgsConstructor
public class AdminUserListRes {

    private Long userId;
    private String email;
    private String nickname;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private int reportCount;

    public static AdminUserListRes from(User user, int reportCount) {
        return new AdminUserListRes(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                reportCount
        );
    }
}
