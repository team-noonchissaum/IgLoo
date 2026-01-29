package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.report.entity.Report;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReportRes {

    private Long reportId;
    private Long reporterId;
    private Long targetUserId;
    private String reason;
    private String status;
    private LocalDateTime createdAt;

    public static ReportRes from(Report report) {
        return new ReportRes(
                report.getId(),
                report.getReporter().getId(),
                report.getTargetId(),
                report.getReason(),
                report.getStatus().name(),
                report.getCreatedAt()
        );
    }
}
