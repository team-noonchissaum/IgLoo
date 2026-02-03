package noonchissaum.backend.domain.user.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.service.AuctionService;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.repository.ItemRepository;
import noonchissaum.backend.domain.order.service.OrderService;
import noonchissaum.backend.domain.report.entity.Report;
import noonchissaum.backend.domain.report.entity.ReportStatus;
import noonchissaum.backend.domain.report.entity.ReportTargetType;
import noonchissaum.backend.domain.report.repository.ReportRepository;
import noonchissaum.backend.domain.user.dto.request.AdminReportProcessReq;
import noonchissaum.backend.domain.user.dto.response.*;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.*;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final AuctionService auctionService;
    private final OrderService orderService;
    private final WalletService walletService;

    /* ================= 신고 관리 ================= */

    /**
     * 신고 목록 조회
     */

    public Page<AdminReportListRes> getReports(String status, String targetType, Pageable pageable) {
        Page<Report> reports = (status == null)
                ? reportRepository.findAllWithReporter(pageable)
                : reportRepository.findByStatusWithReporter(ReportStatus.valueOf(status), pageable);

        return reports.map(report -> new AdminReportListRes(
                report.getId(),
                report.getReporter().getId(),
                report.getReporter().getNickname(),
                report.getTargetType().name(),
                report.getTargetId(),
                report.getReason(),
                report.getStatus().name(),
                report.getCreatedAt()
        ));
    }

    /**
     * 신고 상세 조회
     */

    public AdminReportDetailRes getReportDetail(Long reportId) {
        Report report = reportRepository.findByIdWithReporter(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        AdminReportDetailRes.ReporterInfo reporterInfo = new AdminReportDetailRes.ReporterInfo(
                report.getReporter().getId(),
                report.getReporter().getNickname(),
                report.getReporter().getEmail()
        );

        return new AdminReportDetailRes(
                report.getId(),
                reporterInfo,
                report.getTargetType().name(),
                report.getTargetId(),
                null,  // targetInfo는 targetType에 따라서 조회가 필요함
                report.getReason(),
                report.getStatus().name(),
                report.getCreatedAt()
        );
    }

    /**
     * 신고 처리
     */

    @Transactional
    public void processReport(Long reportId, AdminReportProcessReq req) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        // 이미 처리된 신고인지 체크
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_PROCESSED);
        }

        report.process(ReportStatus.valueOf(req.getStatus()));
    }

    /* ================= 사용자 관리 ================= */
    /**유저 차단*/

    @Transactional
    public AdminBlockUserRes blockUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 이미 차단된 사용자인지 체크
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new CustomException(ErrorCode.USER_ALREADY_BLOCKED);
        }

        user.block(reason);

        return AdminBlockUserRes.from(user);
    }

    /**유저 차단 해제*/
    @Transactional
    public void unblockUser(Long userId) {
        User user = userRepository.findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 차단되지 않은 사용자인지 체크
        if (user.getStatus() != UserStatus.BLOCKED) {
            throw new CustomException(ErrorCode.USER_NOT_BLOCKED);
        }

        user.unblock();
    }

    /**유저 목록 조회*/
    public Page<AdminUserListRes> getUsers(String status, String keyword, Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);

        return users.map(user -> {
            int reportCount = (int) reportRepository.countByTargetTypeAndTargetId(
                    ReportTargetType.USER,
                    user.getId()
            );
            return AdminUserListRes.from(user, reportCount);
        });
    }

    /* ================= 게시글 관리(수정 필요) ================= */

    /**
     * 차단된 게시글 목록 조회
     */

    public Page<AdminItemListRes> getBlockedItems(Pageable pageable) {
        // 추가 필요! Item 상태가 BLOCKED 인 것만 조회
        return Page.empty();
    }

    /**
     * 차단된 게시글 복구
     */

    @Transactional
    public void restoreItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        item.restore();
}

    /* =================== 일일 통계 ==================== */

    public AdminStatisticsRes getDailyStatistics(String date) {
        LocalDate targetDate = (date == null) ? LocalDate.now() : LocalDate.parse(date);

        // 거래 통계
        long orderTotal = orderService.countByDate(targetDate);
        long orderCompleted = orderService.countCompletedByDate(targetDate);
        long orderCanceled = orderService.countCanceledByDate(targetDate);

        AdminStatisticsRes.TransactionStats transaction =
                new AdminStatisticsRes.TransactionStats(
                        (int) orderTotal,
                        (int) orderCompleted,
                        (int) orderCanceled
                );

        // 경매 통계
        long auctionTotal = auctionService.countByDate(targetDate);
        long auctionSuccess = auctionService.countSuccessByDate(targetDate);
        long auctionFailed = auctionService.countFailedByDate(targetDate);
        double successRate = (auctionTotal > 0) ? (double) auctionSuccess / auctionTotal * 100 : 0.0;

        AdminStatisticsRes.AuctionStats auction =
                new AdminStatisticsRes.AuctionStats(
                        (int) auctionTotal,
                        (int) auctionSuccess,
                        (int) auctionFailed,
                        Math.round(successRate * 10) / 10.0  // 소수점 1자리
                );

        // 크레딧 통계
        long totalCharged = walletService.sumChargeByDate(targetDate);
        long totalUsed = walletService.sumUsedByDate(targetDate);
        long totalWithdrawn = walletService.sumWithdrawnByDate(targetDate);

        AdminStatisticsRes.CreditStats credit =
                new AdminStatisticsRes.CreditStats(totalCharged, totalUsed, totalWithdrawn);

        return new AdminStatisticsRes(
                targetDate.toString(),
                transaction,
                auction,
                credit
        );
    }

}
