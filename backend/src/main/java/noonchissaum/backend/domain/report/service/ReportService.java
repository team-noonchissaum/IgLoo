package noonchissaum.backend.domain.report.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.auction.service.AuctionRedisService;
import noonchissaum.backend.domain.notification.constants.NotificationConstants;
import noonchissaum.backend.domain.notification.entity.NotificationType;
import noonchissaum.backend.domain.notification.service.AuctionNotificationService;
import noonchissaum.backend.domain.report.dto.ReportReq;
import noonchissaum.backend.domain.report.entity.Report;
import noonchissaum.backend.domain.report.entity.ReportStatus;
import noonchissaum.backend.domain.report.entity.ReportTargetType;
import noonchissaum.backend.domain.report.repository.ReportRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.handler.ReportTargetHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final Map<ReportTargetType, ReportTargetHandler> handlerMap;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionRedisService auctionRedisService;
    private final AuctionNotificationService auctionNotificationService;

    /** 신고 생성-유저*/
    @Transactional
    public void createReport(Long LoginUserId, ReportReq req) {

        /** 신고자 조회*/
        User reporter = userRepository.findById(LoginUserId).orElseThrow(()->new CustomException(ErrorCode.USER_NOT_FOUND));

        /** 중복 신고 방지*/
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatusIn(
                reporter.getId(),
                req.getTargetType(),
                req.getTargetId(),
                List.of(ReportStatus.PENDING, ReportStatus.PROCESSED)
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

        if (req.getTargetType() == ReportTargetType.AUCTION) {
            handleAuctionReported(req.getTargetId());
        }
    }

    private void handleAuctionReported(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_AUCTIONS));

        if (auction.getStatus() != AuctionStatus.READY &&
                auction.getStatus() != AuctionStatus.RUNNING &&
                auction.getStatus() != AuctionStatus.DEADLINE) {
            return;
        }

        if (auction.getItem() == null) {
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }

        auction.getItem().delete();
        auction.tempBlock();
        auctionRedisService.setRedis(auction.getId());
        notifyAuctionParticipants(
                auctionId,
                NotificationType.AUCTION_TEMP_BLOCKED,
                NotificationConstants.MSG_AUCTION_TEMP_BLOCKED
        );
    }

    private void notifyAuctionParticipants(Long auctionId, NotificationType type, String message) {
        List<Long> participantIds = bidRepository.findDistinctBidderIdsByAuctionId(auctionId);
        for (Long userId : participantIds) {
            auctionNotificationService.sendNotification(
                    userId,
                    type,
                    message,
                    NotificationConstants.REF_TYPE_AUCTION,
                    auctionId
            );
        }
    }
}
