package noonchissaum.backend.domain.user.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.inquiry.dto.res.InquiryListRes;
import noonchissaum.backend.domain.inquiry.service.InquiryService;
import noonchissaum.backend.domain.report.entity.ReportTargetType;
import noonchissaum.backend.domain.user.dto.request.AdminBlockAuctionReq;
import noonchissaum.backend.domain.user.dto.request.AdminBlockUserReq;
import noonchissaum.backend.domain.user.dto.request.AdminReportProcessReq;
import noonchissaum.backend.domain.user.dto.response.*;
import noonchissaum.backend.domain.user.service.AdminService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class AdminController {
    private final AdminService adminService;
    private final InquiryService inquiryService;
    /* ================= 신고 관리 ================= */

    /**
     * 신고 목록 조회
     */
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<Page<AdminReportListRes>>> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            Pageable pageable
    ) {
        Page<AdminReportListRes> result = adminService.getReports(status, targetType, pageable);
        return ResponseEntity.ok(ApiResponse.success("신고 목록 조회 성공", result));
    }

    /**
     * 신고 상세 조회
     */
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<ApiResponse<AdminReportDetailRes>> getReportDetail(@PathVariable Long reportId) {
        AdminReportDetailRes result = adminService.getReportDetail(reportId);
        return ResponseEntity.ok(ApiResponse.success("신고 상세 조회 성공", result));
    }

    /**
     * 신고 처리
     */
    @PatchMapping("/reports/{reportId}")
    public ResponseEntity<ApiResponse<Void>> processReport(
            @PathVariable Long reportId,
            @Valid @RequestBody AdminReportProcessReq req
    ) {
        adminService.processReport(reportId, req);
        return ResponseEntity.ok(ApiResponse.success("신고 처리 완료"));
    }

    /**
     * 특정 대상에 대한 신고 목록 조회
     */
    @GetMapping("/reports/by-target")
    public ResponseEntity<ApiResponse<java.util.List<AdminReportListRes>>> getReportsByTarget(
            @RequestParam String targetType,
            @RequestParam Long targetId
    ) {
        ReportTargetType type = ReportTargetType.valueOf(targetType);
        java.util.List<AdminReportListRes> result = adminService.getReportsByTarget(type, targetId);
        return ResponseEntity.ok(ApiResponse.success("신고 목록 조회 성공", result));
    }

    /* ================== 통계 ====================== */

    /**
     * 일일 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<AdminStatisticsRes>> getStatistics(
            @RequestParam(required = false) String date // 날짜 파라미터
    ) {
        AdminStatisticsRes result = adminService.getDailyStatistics(date);
        return ResponseEntity.ok(ApiResponse.success("통계 조회 성공", result));
    }

    /* ================= 사용자 관리 ================= */

    /**
     * 사용자 목록 조회
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserListRes>>> getUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            Pageable pageable
    ) {
        Page<AdminUserListRes> result = adminService.getUsers(status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("사용자 목록 조회 성공", result));
    }

    /**
     * 사용자 차단
     */
    @PatchMapping("/users/{userId}/block")
    public ResponseEntity<ApiResponse<AdminBlockUserRes>> blockUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminBlockUserReq req
    ) {
        AdminBlockUserRes result = adminService.blockUser(userId, req.getReason());
        return ResponseEntity.ok(ApiResponse.success("사용자 차단 완료", result));
    }

    /**
     * 사용자 차단 해제
     */
    @PatchMapping("/users/{userId}/unblock")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable Long userId) {
        adminService.unblockUser(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자 차단 해제 완료"));
    }

    /**
     * 차단된 사용자 목록 조회
     */
    @GetMapping("/users/blocked")
    public ResponseEntity<ApiResponse<Page<AdminBlockedUserRes>>> getBlockedUsers(Pageable pageable) {
        Page<AdminBlockedUserRes> result = adminService.getBlockedUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success("차단된 사용자 목록 조회 성공", result));
    }

    /* ================= 경매 게시글 관리 ================= */

    /**
     * 경매 차단
     * POST /api/admin/auctions/{auctionId}/block
     */
    @PostMapping("/auctions/{auctionId}/block")
    public ResponseEntity<ApiResponse<AdminAuctionBlockRes>> blockAuction(
            @PathVariable Long auctionId,
            @RequestBody AdminBlockAuctionReq request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AdminAuctionBlockRes result = adminService.blockAuction(auctionId, request.getReason(), principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("경매 차단 완료", result));
    }

    /**
     * 차단된 경매 게시글 목록 조회
     */
    @GetMapping("/auctions/blocked")
    public ResponseEntity<ApiResponse<Page<AdminBlockedAuctionRes>>> getBlockedAuctions(Pageable pageable) {
        Page<AdminBlockedAuctionRes> result = adminService.getBlockedAuctions(pageable);
        return ResponseEntity.ok(ApiResponse.success("차단된 경매 목록 조회 성공", result));
    }

    /**
     * 차단된 경매 복구
     * PATCH /api/admin/auctions/{auctionId}/restore
     */
    @PatchMapping("/auctions/{auctionId}/restore")
    public ResponseEntity<ApiResponse<AdminAuctionRestoreRes>> restoreAuction(
            @PathVariable Long auctionId
    ) {
        AdminAuctionRestoreRes result = adminService.restoreAuction(auctionId);
        return ResponseEntity.ok(ApiResponse.success("경매 복구 완료", result));
    }

    /* ================= 문의 및 유저 차단 관련 ===================== */

    /**
     * 차단 해제 요청 목록 조회
     */
    @GetMapping("/inquiries")
    public ResponseEntity<ApiResponse<Page<InquiryListRes>>> getInquiries(Pageable pageable) {
        Page<InquiryListRes> result = inquiryService.getInquiries(pageable);
        return ResponseEntity.ok(ApiResponse.success("차단 해제 요청 목록 조회 성공",result));
    }

    /**
     * 닉네임으로 유저 차단 해제
     */
    @PatchMapping("/users/unblock-by-nickname")
    public ResponseEntity<ApiResponse<Void>> unblockUserByNickname(
            @RequestParam String nickname
    ) {
        adminService.unblockUserByNickname(nickname);
        return ResponseEntity.ok(ApiResponse.success("차단 해제 완료"));
    }
}
