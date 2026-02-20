package noonchissaum.backend.domain.report.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.report.dto.ReportReq;
import noonchissaum.backend.domain.report.service.ReportService;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<Void> createReport(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid ReportReq req
    ) {
        reportService.createReport(
                userPrincipal.getUserId(),
                req
        );        return ResponseEntity.ok().build();
    }
}