package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 신고 목록 조회 응답
 */

@Getter
@AllArgsConstructor
public class AdminReportListRes {

    private Long reportId;
    private Long reporterId;
    private String reporterNickname;
    private String targetType;
    private Long targetId;
    private String targetName; // 유저일 경우 닉네임, 경매일 경우 제목
    private String reason;
    private String status;
    private LocalDateTime createdAt;
}
