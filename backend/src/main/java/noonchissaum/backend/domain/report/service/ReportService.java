package noonchissaum.backend.domain.report.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.report.dto.ReportReq;
import noonchissaum.backend.domain.report.entity.Report;
import noonchissaum.backend.domain.report.entity.ReportTargetType;
import noonchissaum.backend.domain.report.repository.ReportRepository;
import noonchissaum.backend.domain.user.dto.response.AdminReportListRes;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final Map<ReportTargetType, ReportTargetHandler> handlerMap;

    /** 신고 생성-유저*/
    @Transactional
    public void createReport(Long LoginUserId, ReportReq req) {

        /** 신고자 조회*/
        User reporter = userRepository.findById(LoginUserId).orElseThrow(()->new CustomException(ErrorCode.USER_NOT_FOUND));

        /** 중복 신고 방지*/
        if(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporter.getId(),
                req.getTargetType(),
                req.getTargetId()
        )) {
            throw new CustomException(ErrorCode.ALREADY_REPORTED);
        }

        /**신고 대상자 검증*/
        ReportTargetHandler handler = handlerMap.get(req.getTargetType());
        if(handler==null) {
            throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);//신고대상오류
        }

        handler.validate(req.getTargetId());

        /**신고 생성*/
        Report report=Report.create(
                reporter,
                req.getTargetType(),
                req.getTargetId(),
                req.getReason(),
                req.getDescription()
        );
        reportRepository.save(report);
    }
}
