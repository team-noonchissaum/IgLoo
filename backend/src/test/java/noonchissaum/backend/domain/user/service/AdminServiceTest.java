package noonchissaum.backend.domain.user.service;

import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.auction.service.AuctionRedisService;
import noonchissaum.backend.domain.auction.service.AuctionService;
import noonchissaum.backend.domain.auction.service.BidRollbackService;
import noonchissaum.backend.domain.notification.service.AuctionNotificationService;
import noonchissaum.backend.domain.order.service.OrderService;
import noonchissaum.backend.domain.report.entity.Report;
import noonchissaum.backend.domain.report.entity.ReportStatus;
import noonchissaum.backend.domain.report.entity.ReportTargetType;
import noonchissaum.backend.domain.report.repository.ReportRepository;
import noonchissaum.backend.domain.user.dto.request.AdminReportProcessReq;
import noonchissaum.backend.domain.user.dto.response.AdminBlockUserRes;
import noonchissaum.backend.domain.user.dto.response.AdminReportDetailRes;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.wallet.service.WalletRecordService;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.domain.inquiry.service.InquiryService;
import noonchissaum.backend.domain.statistics.repository.DailyStatisticsRepository;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.util.UserLockExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuctionRepository auctionRepository;
    @Mock private BidRepository bidRepository;
    @Mock private AuctionService auctionService;
    @Mock private AuctionRedisService auctionRedisService;
    @Mock private OrderService orderService;
    @Mock private WalletService walletService;
    @Mock private WalletRecordService walletRecordService;
    @Mock private AuctionNotificationService auctionNotificationService;
    @Mock private UserLockExecutor userLockExecutor;
    @Mock private InquiryService inquiryService;
    @Mock private DailyStatisticsRepository dailyStatisticsRepository;
    @Mock private BidRollbackService bidRollbackService;

    @InjectMocks
    private AdminService adminService;

    /**
     * 유저 차단
     */
    @Test
    void blockUser() {
        // given
        Long userId = 1L;
        String reason = "욕설";

        User user = User.builder()
                .email("test@test.com")
                .nickname("baduser")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        // when
        AdminBlockUserRes result = adminService.blockUser(userId, reason);

        // then
        assertEquals(userId, result.getUserId());
        verify(bidRollbackService, times(1)).rollbackAuctionsForBlockedUser(userId);
    }

    /**
     * 유저 차단 - 유저 없음 예외
     */
    @Test
    void blockUser_userNotFound_throwsException() {
        // given
        Long userId = 999L;

        given(userRepository.findById(userId))
                .willReturn(Optional.empty());

        // when & then
        assertThrows(ApiException.class, () ->
                adminService.blockUser(userId, "사유"));
    }

    /**
     * 유저 차단 - 이미 차단됨 예외
     */
    @Test
    void blockUser_alreadyBlocked_throwsException() {
        // given
        Long userId = 1L;

        User user = User.builder()
                .email("test@test.com")
                .nickname("blockeduser")
                .role(UserRole.USER)
                .status(UserStatus.BLOCKED)
                .build();

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        // when & then
        assertThrows(ApiException.class, () ->
                adminService.blockUser(userId, "사유"));
    }

    /* ================= 유저 차단 해제 ================= */

    /**
     * 유저 차단 해제
     */
    @Test
    void unblockUser() {
        // given
        Long userId = 1L;

        User user = User.builder()
                .email("test@test.com")
                .nickname("blockeduser")
                .role(UserRole.USER)
                .status(UserStatus.BLOCKED)
                .build();

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        // when
        adminService.unblockUser(userId);

        // then
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    /**
     * 유저 차단 해제 - 차단 아님 예외
     */
    @Test
    void unblockUser_notBlocked_throwsException() {
        // given
        Long userId = 1L;

        User user = User.builder()
                .email("test@test.com")
                .nickname("activeuser")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        given(userRepository.findById(userId))
                .willReturn(Optional.of(user));

        // when & then
        assertThrows(ApiException.class, () ->
                adminService.unblockUser(userId));
    }

    /* ================= 신고 처리 ================= */

    /**
     * 신고 처리
     */
    @Test
    void processReport() {
        // given
        Long reportId = 1L;

        User reporter = User.builder()
                .email("reporter@test.com")
                .nickname("reporter")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(ReportTargetType.USER)
                .targetId(2L)
                .reason("욕설")
                .status(ReportStatus.PENDING)
                .build();

        AdminReportProcessReq req = new AdminReportProcessReq();
        ReflectionTestUtils.setField(req, "status", "PROCESSED");
        ReflectionTestUtils.setField(req, "processResult", "경고 처리");
        ReflectionTestUtils.setField(req, "blockTarget", false);

        given(reportRepository.findById(reportId))
                .willReturn(Optional.of(report));

        // when
        adminService.processReport(reportId, req);

        // then
        assertEquals(ReportStatus.PROCESSED, report.getStatus());
    }

    /**
     * 신고 처리 - 신고 없음 예외
     */
    @Test
    void processReport_notFound_throwsException() {
        // given
        Long reportId = 999L;

        AdminReportProcessReq req = new AdminReportProcessReq();
        ReflectionTestUtils.setField(req, "status", "PROCESSED");

        given(reportRepository.findById(reportId))
                .willReturn(Optional.empty());

        // when & then
        assertThrows(ApiException.class, () ->
                adminService.processReport(reportId, req));
    }

    /**
     * 신고 처리 - 이미 처리됨 예외
     */
    @Test
    void processReport_alreadyProcessed_throwsException() {
        // given
        Long reportId = 1L;

        User reporter = User.builder()
                .email("reporter@test.com")
                .nickname("reporter")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(ReportTargetType.USER)
                .targetId(2L)
                .reason("욕설")
                .status(ReportStatus.PROCESSED)  // 이미 처리됨
                .build();

        AdminReportProcessReq req = new AdminReportProcessReq();
        ReflectionTestUtils.setField(req, "status", "PROCESSED");

        given(reportRepository.findById(reportId))
                .willReturn(Optional.of(report));

        // when & then
        assertThrows(ApiException.class, () ->
                adminService.processReport(reportId, req));
    }

    /* ================= 신고 상세 조회 ================= */

    /**
     * 신고 상세 조회
     */
    @Test
    void getReportDetail() {
        // given
        Long reportId = 1L;

        User reporter = User.builder()
                .email("reporter@test.com")
                .nickname("reporter")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(reporter, "id", 1L);

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(ReportTargetType.USER)
                .targetId(2L)
                .reason("욕설")
                .status(ReportStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(report, "id", reportId);

        User targetUser = User.builder()
                .email("target@test.com")
                .nickname("targetuser")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        given(reportRepository.findByIdWithReporter(reportId))
                .willReturn(Optional.of(report));
        given(userRepository.findById(2L))
                .willReturn(Optional.of(targetUser));

        // when
        AdminReportDetailRes result = adminService.getReportDetail(reportId);

        // then
        assertEquals(reportId, result.getReportId());
        assertEquals("reporter", result.getReporter().getNickname());
    }

    /**
     * 신고 상세 조회 - 신고 없음 예외
     */
    @Test
    void getReportDetail_notFound_throwsException() {
        // given
        Long reportId = 999L;

        given(reportRepository.findByIdWithReporter(reportId))
                .willReturn(Optional.empty());

        // when & then
        assertThrows(ApiException.class, () ->
                adminService.getReportDetail(reportId));
    }
}
