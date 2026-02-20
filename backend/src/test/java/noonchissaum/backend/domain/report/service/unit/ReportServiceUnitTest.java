package noonchissaum.backend.domain.report.service.unit;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.auction.service.AuctionRedisService;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.notification.service.AuctionNotificationService;
import noonchissaum.backend.domain.report.dto.ReportReq;
import noonchissaum.backend.domain.report.entity.ReportStatus;
import noonchissaum.backend.domain.report.entity.ReportTargetType;
import noonchissaum.backend.domain.report.repository.ReportRepository;
import noonchissaum.backend.domain.report.service.ReportService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.handler.ReportTargetHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ReportServiceUnitTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private BidRepository bidRepository;
    @Mock
    private AuctionRedisService auctionRedisService;
    @Mock
    private AuctionNotificationService auctionNotificationService;

    @Mock
    private ReportTargetHandler auctionHandler;

    @Test
    @DisplayName("신고 생성 시 중복 신고가 있으면 ALREADY_REPORTED 예외 던짐")
    void createReport_whenAlreadyReported_throwsApiException() {
        ReportService service = createService();
        User reporter = sampleUser(1L, "reporter");
        ReportReq req = reportReq(ReportTargetType.AUCTION, 100L, "사유", "상세");
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
                1L, ReportTargetType.AUCTION, 100L, List.of(ReportStatus.PENDING, ReportStatus.PROCESSED)))
                .thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.createReport(1L, req));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ALREADY_REPORTED);
        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("경매 신고 생성 시 대상 검증 후 임시 차단 및 알림 처리")
    void createReport_forAuction_tempBlocksAuctionAndNotifies() {
        ReportService service = createService();
        User reporter = sampleUser(2L, "reporter2");
        Auction auction = sampleAuction(200L, 9L, "report-auction");
        ReportReq req = reportReq(ReportTargetType.AUCTION, 200L, "허위매물", "신고");
        when(userRepository.findById(2L)).thenReturn(Optional.of(reporter));
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
                2L, ReportTargetType.AUCTION, 200L, List.of(ReportStatus.PENDING, ReportStatus.PROCESSED)))
                .thenReturn(false);
        when(auctionRepository.findById(200L)).thenReturn(Optional.of(auction));
        when(bidRepository.findDistinctBidderIdsByAuctionId(200L)).thenReturn(new ArrayList<>(List.of(30L, 31L)));

        service.createReport(2L, req);

        verify(auctionHandler).validate(200L);
        verify(reportRepository).save(any());
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.TEMP_BLOCKED);
        verify(auctionRedisService).setRedis(200L);
        verify(auctionNotificationService).sendNotification(org.mockito.ArgumentMatchers.eq(30L), any(), any(), any(), org.mockito.ArgumentMatchers.eq(200L));
        verify(auctionNotificationService).sendNotification(org.mockito.ArgumentMatchers.eq(31L), any(), any(), any(), org.mockito.ArgumentMatchers.eq(200L));
        verify(auctionNotificationService).sendNotification(org.mockito.ArgumentMatchers.eq(9L), any(), any(), any(), org.mockito.ArgumentMatchers.eq(200L));
    }

    private ReportService createService() {
        return new ReportService(
                reportRepository,
                userRepository,
                Map.of(ReportTargetType.AUCTION, auctionHandler),
                auctionRepository,
                bidRepository,
                auctionRedisService,
                auctionNotificationService
        );
    }

    private User sampleUser(Long userId, String suffix) {
        User user = User.builder()
                .email("report-service-unit-" + suffix + "@test.com")
                .nickname("report_service_unit_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private Auction sampleAuction(Long auctionId, Long sellerId, String suffix) {
        User seller = sampleUser(sellerId, "seller-" + suffix);
        Category category = new Category("category-" + suffix, null);
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title("title-" + suffix)
                .description("desc")
                .build();

        Auction auction = Auction.builder()
                .item(item)
                .startPrice(BigDecimal.valueOf(10000))
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(auction, "id", auctionId);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.RUNNING);
        return auction;
    }

    private ReportReq reportReq(ReportTargetType type, Long targetId, String reason, String description) {
        ReportReq req = new ReportReq();
        ReflectionTestUtils.setField(req, "targetType", type);
        ReflectionTestUtils.setField(req, "targetId", targetId);
        ReflectionTestUtils.setField(req, "reason", reason);
        ReflectionTestUtils.setField(req, "description", description);
        return req;
    }
}

