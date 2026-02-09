package noonchissaum.backend.domain.user.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.inquiry.service.InquiryService;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.report.entity.Report;
import noonchissaum.backend.domain.report.entity.ReportStatus;
import noonchissaum.backend.domain.report.entity.ReportTargetType;
import noonchissaum.backend.domain.report.repository.ReportRepository;
import noonchissaum.backend.domain.statistics.repository.DailyStatisticsRepository;
import noonchissaum.backend.domain.user.dto.request.AdminReportProcessReq;
import noonchissaum.backend.domain.user.dto.response.*;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.*;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;
    private final OrderService orderService;
    private final WalletService walletService;
    private final InquiryService inquiryService;
    private final DailyStatisticsRepository dailyStatisticsRepository;

    /* ================= 신고 관리 ================= */

    /**
     * 신고 목록 조회
     */
    public Page<AdminReportListRes> getReports(String status, String targetType, Pageable pageable) {
        Page<Report> reports;
        if (targetType != null && !targetType.isBlank()) {
            ReportTargetType type = ReportTargetType.valueOf(targetType);
            ReportStatus statusFilter = (status != null && !status.isBlank())
                    ? ReportStatus.valueOf(status)
                    : ReportStatus.PENDING;
            reports = reportRepository.findByStatusAndTargetTypeWithReporter(statusFilter, type, pageable);
        } else if (status != null && !status.isBlank()) {
            reports = reportRepository.findByStatusWithReporter(ReportStatus.valueOf(status), pageable);
        } else {
            reports = reportRepository.findAllWithReporter(pageable);
        }

        return reports.map(report -> {
            String targetName = getTargetName(report.getTargetType(), report.getTargetId());

            return new AdminReportListRes(
                    report.getId(),
                    report.getReporter().getId(),
                    report.getReporter().getNickname(),
                    report.getTargetType().name(),
                    report.getTargetId(),
                    targetName,
                    report.getReason(),
                    report.getStatus().name(),
                    report.getCreatedAt()
            );
        });
    }

    /**
     * 특정 대상에 대한 신고 목록 조회
     */
    public List<AdminReportListRes> getReportsByTarget(ReportTargetType targetType, Long targetId) {
        List<Report> reports = reportRepository.findByTargetTypeAndTargetId(targetType, targetId);

        return reports.stream().map(report -> {
            String targetName = getTargetName(report.getTargetType(), report.getTargetId());

            return new AdminReportListRes(
                    report.getId(),
                    report.getReporter().getId(),
                    report.getReporter().getNickname(),
                    report.getTargetType().name(),
                    report.getTargetId(),
                    targetName,
                    report.getReason(),
                    report.getStatus().name(),
                    report.getCreatedAt()
            );
        }).collect(Collectors.toList());
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

        // [추가] targetInfo 조회
        String targetInfo = getTargetName(report.getTargetType(), report.getTargetId());

        return new AdminReportDetailRes(
                report.getId(),
                reporterInfo,
                report.getTargetType().name(),
                report.getTargetId(),
                targetInfo,  // [변경] null → targetInfo
                report.getReason(),
                report.getStatus().name(),
                report.getCreatedAt()
        );
    }

    /**
     * 신고 대상 이름 조회
     */
    private String getTargetName(ReportTargetType targetType, Long targetId) {
        if (targetType == ReportTargetType.USER) {
            return userRepository.findById(targetId)
                    .map(user -> user.getNickname() + " (userId: " + targetId + ")")
                    .orElse("삭제된 사용자 (userId: " + targetId + ")");
        } else {
            return auctionRepository.findById(targetId)
                    .map(auction -> auction.getItem().getTitle() + " (auctionId: " + targetId + ")")
                    .orElse("삭제된 경매 (auctionId: " + targetId + ")");
        }
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

        if (req.getProcessResult() != null && !req.getProcessResult().isBlank()) {
            report.addProcessResult(req.getProcessResult());
        }

        if (Boolean.TRUE.equals(req.getBlockTarget())) {
            blockReportTarget(report, req.getProcessResult());
        }
    }

    /**신고 대상 제재 처리*/
    private void blockReportTarget(Report report, String reason) {
        ReportTargetType targetType = report.getTargetType();
        Long targetId = report.getTargetId();

        switch (targetType) {
            case USER:
                blockUser(targetId, reason);
                break;
            case AUCTION:
                blockAuctionByReport(targetId, reason);
                break;
            default:
                throw new CustomException(ErrorCode.INVALID_REPORT_TARGET);
        }
    }

    /* ================= 경매 게시글 관리 ================= */

    /**
     * 경매 차단 공통 로직
     */
    private void doBlockAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_AUCTIONS));

        Item item = auction.getItem();
        if (item == null) {
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }

        if (item.isDeleted()) {
            throw new CustomException(ErrorCode.ITEM_ALREADY_BLOCKED);
        }

        // 진행 중인 경매만 차단 가능
        if (auction.getStatus() != AuctionStatus.READY &&
                auction.getStatus() != AuctionStatus.RUNNING &&
                auction.getStatus() != AuctionStatus.DEADLINE)
        {
            throw new CustomException(ErrorCode.AUCTION_CANNOT_BLOCK);
        }

        item.delete();
        auction.block();
    }

    /**
     * 신고로 인한 경매 차단
     */
    private void blockAuctionByReport(Long auctionId, String reason) {
        doBlockAuction(auctionId);
        log.info("신고 처리로 경매 차단 완료 - auctionId: {}, reason: {}", auctionId, reason);
    }

    /**
     * 경매 게시글 차단 (관리자 직접)
     */
    @Transactional
    public AdminAuctionBlockRes blockAuction(Long auctionId, String reason, Long adminId) {
        doBlockAuction(auctionId);

        // 해당 경매의 PENDING 신고가 있는지 확인
        long pendingCount = reportRepository.countByTargetTypeAndTargetIdAndStatus(
                ReportTargetType.AUCTION, auctionId, ReportStatus.PENDING);

        if (pendingCount > 0) {
            reportRepository.updateStatusByTargetTypeAndTargetIdAndStatus(
                    ReportTargetType.AUCTION,
                    auctionId,
                    ReportStatus.PENDING,
                    ReportStatus.PROCESSED
            );
        } else {
            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            Report report = Report.builder()
                    .reporter(admin)
                    .targetType(ReportTargetType.AUCTION)
                    .targetId(auctionId)
                    .reason(reason)
                    .description("관리자 직접 차단")
                    .status(ReportStatus.PROCESSED)
                    .processedAt(LocalDateTime.now())
                    .build();

            reportRepository.save(report);
        }

        log.info("경매 게시글 차단 완료 - auctionId: {}, reason: {}", auctionId, reason);

        return new AdminAuctionBlockRes(
                auctionId,
                "BLOCKED",
                LocalDateTime.now()
        );
    }

    /**
     * 차단된 경매 게시글 복구
     */
    @Transactional
    public AdminAuctionRestoreRes restoreAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_AUCTIONS));

        Item item = auction.getItem();
        if (item == null) {
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }

        if (!item.isDeleted()) {
            throw new CustomException(ErrorCode.ITEM_NOT_BLOCKED);
        }

        if (auction.getStatus() != AuctionStatus.BLOCKED) {
            throw new CustomException(ErrorCode.AUCTION_NOT_BLOCKED);
        }

        item.restore();
        auction.reopen();

        reportRepository.updateStatusByTargetTypeAndTargetIdAndStatus(
                ReportTargetType.AUCTION,
                auctionId,
                ReportStatus.PROCESSED,
                ReportStatus.REJECTED
        );

        log.info("경매 게시글 복구 완료 - auctionId: {}", auctionId);

        return new AdminAuctionRestoreRes(
                auctionId,
                "ACTIVE",
                LocalDateTime.now()
        );
    }

    /**
     * 차단된 경매 게시글 목록 조회
     */
    public Page<AdminBlockedAuctionRes> getBlockedAuctions(Pageable pageable) {
        List<Auction> allAuctions = auctionRepository.findAll();

        List<AdminBlockedAuctionRes> blockedList = allAuctions.stream()
                .filter(auction -> auction.getStatus() == AuctionStatus.BLOCKED)
                .map(auction -> {
                    Item item = auction.getItem();

                    Report report = reportRepository
                            .findTopByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                                    ReportTargetType.AUCTION, auction.getId())
                            .orElse(null);

                    String reason = (report != null) ? report.getReason() : "관리자 직접 차단";
                    LocalDateTime blockedAt = (report != null) ? report.getProcessedAt() : item.getUpdatedAt();

                    return new AdminBlockedAuctionRes(
                            auction.getId(),
                            item.getTitle(),
                            item.getSeller().getId(),
                            item.getSeller().getNickname(),
                            reason,
                            blockedAt
                    );
                })
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), blockedList.size());

        List<AdminBlockedAuctionRes> pagedList = (start >= blockedList.size())
                ? Collections.emptyList()
                : blockedList.subList(start, end);

        return new PageImpl<>(pagedList, pageable, blockedList.size());
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

    /**닉네임으로 유저 차단 해제*/
    @Transactional
    public void unblockUserByNickname(String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 차단되지 않은 사용자인 경우 요청만 삭제하고 종료
        if (user.getStatus() != UserStatus.BLOCKED) {
            inquiryService.deleteByNickname(nickname);
            return;
        }

        // 차단된 사용자인 경우 차단 해제 수행
        unblockUser(user.getId());
        // 차단 해제 성공 시 해당 닉네임의 차단 해제 요청 삭제
        inquiryService.deleteByNickname(nickname);
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

    /** 차단된 사용자 목록 조회 */
    public Page<AdminBlockedUserRes> getBlockedUsers(Pageable pageable) {
        return userRepository.findByStatus(UserStatus.BLOCKED, pageable)
                .map(AdminBlockedUserRes::from);
    }

    /**
     * 일일 통계 조회
     * date가 없으면 어제 날짜 조회 (배치가 어제 데이터를 집계)
     * date가 있으면 해당 날짜 조회
     */

    public AdminStatisticsRes getDailyStatistics(String date) {
        LocalDate targetDate = (date == null || date.isBlank())
                ? LocalDate.now().minusDays(1)
                : LocalDate.parse(date);

        return dailyStatisticsRepository.findByStatDate(targetDate)
                .map(AdminStatisticsRes::from)
                .orElse(null);
    }

}
