package noonchissaum.backend.domain.user.controller;


import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.user.dto.request.ReportProcessReq;
import noonchissaum.backend.domain.user.dto.response.ReportRes;
import noonchissaum.backend.domain.user.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    /* ================= 신고 관리 ================= */

    @GetMapping("/reports")
    public List<ReportRes> getReports(
            @RequestParam(required = false) String status
    ) {
        return adminService.getReports(status);
    }

    @PatchMapping("/reports/{reportId}")
    public void processReport(
            @PathVariable Long reportId,
            @RequestBody ReportProcessReq req
    ) {
        adminService.processReport(reportId, req);
    }

    /* ================= 사용자 관리 ================= */

    @PatchMapping("/users/{userId}/block")
    public void blockUser(@PathVariable Long userId) {
        adminService.blockUser(userId);
    }

    @PatchMapping("/users/{userId}/unblock")
    public void unblockUser(@PathVariable Long userId) {
        adminService.unblockUser(userId);
    }

    /* ================= 게시글 관리 ================= */

    @PatchMapping("/items/{itemId}/restore")
    public void restoreItem(@PathVariable Long itemId) {
        adminService.restoreItem(itemId);
    }

}
