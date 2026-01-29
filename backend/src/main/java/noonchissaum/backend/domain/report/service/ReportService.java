package noonchissaum.backend.domain.report.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.report.dto.ReportReq;
import noonchissaum.backend.domain.report.entity.Report;
import noonchissaum.backend.domain.report.entity.ReportTargetType;
import noonchissaum.backend.domain.report.repository.ReportRepository;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.handler.ReportTargetHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {
    private final ReportRepository reportRepository;
    private final Map<ReportTargetType, ReportTargetHandler> handlerMap;

    /**신고 생성-유저*/
    public void createReport(Long reporterId, ReportReq req) {
        // 1. 대상 handler 조회
        ReportTargetHandler handler = handlerMap.get(req.getTargetType());
        if (handler == null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
        }

        // 2. 신고 대상 유효성 검증
        handler.validate(req.getTargetId());

        // 3. 중복 신고 체크
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporterId, req.getTargetType(), req.getTargetId())) {
            throw new CustomException(ErrorCode.ALREADY_REPORTED);
        }

        // 4. 엔티티 생성
//        Report report = Report.create(
//                reporterId,
//                req.getTargetType(),
//                req.getTargetId()
//        );

//        reportRepository.save(report);
    }
}
