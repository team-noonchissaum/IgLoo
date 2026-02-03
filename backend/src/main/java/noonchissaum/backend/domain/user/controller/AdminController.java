package noonchissaum.backend.domain.user.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.user.dto.request.AdminBlockUserReq;
import noonchissaum.backend.domain.user.dto.request.AdminReportProcessReq;
import noonchissaum.backend.domain.user.dto.response.*;
import noonchissaum.backend.domain.user.service.AdminService;
import noonchissaum.backend.global.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@EnableMethodSecurity(prePostEnabled = true)
public class AdminController {
    private final AdminService adminService;

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

    /* ================= 게시글 관리 ================= */

    /**
     * 차단된 게시글 목록 조회
     */

    @GetMapping("/items/blocked")
    public ResponseEntity<ApiResponse<Page<AdminItemListRes>>> getBlockedItems(Pageable pageable) {
        Page<AdminItemListRes> result = adminService.getBlockedItems(pageable);
        return ResponseEntity.ok(ApiResponse.success("차단된 게시글 목록 조회 성공", result));
    }

    /**
     * 차단된 게시글 복구
     */
    @PatchMapping("/items/{itemId}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreItem(@PathVariable Long itemId) {
        adminService.restoreItem(itemId);
        return ResponseEntity.ok(ApiResponse.success("차단된 게시글 복구 완료"));
    }
}
