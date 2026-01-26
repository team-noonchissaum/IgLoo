package noonchissaum.backend.domain.user.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.user.dto.request.AdminBlockUserReq;
import noonchissaum.backend.domain.user.dto.request.AdminReportProcessReq;
import noonchissaum.backend.domain.user.dto.response.*;
import noonchissaum.backend.domain.user.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    /* ================= 신고 관리 ================= */

    /**
     * 신고 목록 조회
     */

    @GetMapping("/reports")
    public ResponseEntity<Page<AdminReportListRes>> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getReports(status, targetType, pageable));
    }

    /**
     * 신고 상세 조회
     */

    @GetMapping("/reports/{reportId}")
    public ResponseEntity<AdminReportDetailRes> getReportDetail(@PathVariable Long reportId) {
        return ResponseEntity.ok(adminService.getReportDetail(reportId));
    }

    /**
     * 신고 처리
     */

    @PatchMapping("/reports/{reportId}")
    public ResponseEntity<Void> processReport(
            @PathVariable Long reportId,
            @Valid @RequestBody AdminReportProcessReq req
    ) {
        adminService.processReport(reportId, req);
        return ResponseEntity.noContent().build();
    }

    /* ================== 통계 ====================== */

    /**
     * 일일 통계 조회
     */

    @GetMapping("/statistics")
    public ResponseEntity<AdminStatisticsRes> getStatistics(
            @RequestParam(required = false) String date
    ) {
        return ResponseEntity.ok(adminService.getDailyStatistics(date));
    }

    /* ================= 사용자 관리 ================= */

    /**
     * 사용자 목록 조회
     */

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserListRes>> getUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.getUsers(status, keyword, pageable));
    }

    /**
     * 사용자 차단
     */

    @PatchMapping("/users/{userId}/block")
    public ResponseEntity<AdminBlockUserRes> blockUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminBlockUserReq req
    ) {
        return ResponseEntity.ok(adminService.blockUser(userId, req.getReason()));
    }

    /**
     * 사용자 차단 해제
     */

    @PatchMapping("/users/{userId}/unblock")
    public ResponseEntity<Void> unblockUser(@PathVariable Long userId) {
        adminService.unblockUser(userId);
        return ResponseEntity.noContent().build();
    }

    /* ================= 게시글 관리 ================= */

    /**
     * 차단된 게시글 목록 조회
     */

    @GetMapping("/items/blocked")
    public ResponseEntity<Page<AdminItemListRes>> getBlockedItems(Pageable pageable) {
        return ResponseEntity.ok(adminService.getBlockedItems(pageable));
    }

    /**
     * 차단된 게시글 복구
     */
    @PatchMapping("/items/{itemId}/restore")
    public ResponseEntity<Void> restoreItem(@PathVariable Long itemId) {
        adminService.restoreItem(itemId);
        return ResponseEntity.noContent().build();
    }

}
