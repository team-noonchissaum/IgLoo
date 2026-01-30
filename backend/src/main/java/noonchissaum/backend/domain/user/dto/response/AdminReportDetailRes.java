package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 신고 상세 조회 응답
 */

@Getter
@AllArgsConstructor
public class AdminReportDetailRes {

    private Long reportId;
    private ReporterInfo reporter;
    private String targetType;
    private Long targetId;
    private Object targetInfo;
    private String reason;
    private String status;
    private LocalDateTime createdAt;

    @Getter
    @AllArgsConstructor
    public static class ReporterInfo {
        private Long userId;
        private String nickname;
        private String email;
    }
}
