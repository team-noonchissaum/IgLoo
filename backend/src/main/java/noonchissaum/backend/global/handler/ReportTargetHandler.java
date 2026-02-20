package noonchissaum.backend.global.handler;

import noonchissaum.backend.domain.report.entity.ReportTargetType;

public interface ReportTargetHandler {

    //신고 대상 유형
    ReportTargetType getType();

    //신고 대상 유효성 검증
    void validate(Long targetId);
}
