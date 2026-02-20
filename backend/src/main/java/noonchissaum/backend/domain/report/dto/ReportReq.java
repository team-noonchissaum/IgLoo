package noonchissaum.backend.domain.report.dto;

import lombok.Getter;
import noonchissaum.backend.domain.report.entity.ReportTargetType;

@Getter
public class ReportReq {
    private ReportTargetType targetType;
    private Long targetId;
    private String reason;
    private String description;
}
