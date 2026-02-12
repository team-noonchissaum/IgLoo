package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.dto.ws.AuctionEndedPayload;
import noonchissaum.backend.domain.auction.dto.ws.AuctionResultPayload;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.notification.constants.NotificationConstants;
import noonchissaum.backend.domain.notification.entity.NotificationType;
import noonchissaum.backend.domain.notification.service.AuctionNotificationService;
import noonchissaum.backend.domain.order.service.OrderService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuctionSchedulerService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final OrderService orderService;
    private final AuctionRealtimeSnapshotService snapshotService;
    private final AuctionMessageService auctionMessageService;
    private final AuctionNotificationService auctionNotificationService;
    private final AuctionRedisService auctionRedisService;
    private final WalletService walletService;


    /**
     * 진행 중인 모든 경매의 상태를 실시간으로 중계합니다. (1초 주기)
     */
    @Transactional(readOnly = true)
    public void broadcastActiveAuctions() {
        List<AuctionStatus> activeStatuses = List.of(AuctionStatus.RUNNING, AuctionStatus.DEADLINE);
        List<Auction> activeAuctions = auctionRepository.findAllByStatusIn(activeStatuses);

        for (Auction auction : activeAuctions) {
            try {
                var snapshot = snapshotService.getSnapshot(auction.getId());
                auctionMessageService.sendAuctionSnapshot(auction.getId(), snapshot);
            } catch (Exception e) {
                log.error("Failed to broadcast auction snapshot for auctionId: {}", auction.getId(), e);
            }
        }
    }

    /**
     * READY -> RUNNING
     */
    @Transactional
    public int expose(LocalDateTime now) {
        LocalDateTime threshold = now.minusMinutes(5);

        long startMs = System.currentTimeMillis();

        List<Auction> auctions = auctionRepository.findReadyAuctions(AuctionStatus.READY, threshold)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_AUCTIONS));
        int updated=0;
        for(Auction auction :auctions){
            auction.run();
            int amount = (int) Math.min( auction.getCurrentPrice().longValue() * 0.05 , 1000);
            walletService.setAuctionDeposit(auction.getItem().getSeller().getId(), auction.getId(), amount, "refund");
            auctionRedisService.setRedis(auction.getId());
            updated++;
        }
        long elapsed = System.currentTimeMillis() - startMs;

        if (updated > 0) {
            log.info("[AuctionExpose] running={} elapsedMs={} threshold={}", updated, elapsed, threshold);
        } else {
            log.debug("[AuctionExpose] running=0 elapsedMs={}", elapsed);
        }

        return updated;
    }

    /**
     * RUNNING -> DEADLINE
     */
    @Transactional
    public void markDeadline() {
        LocalDateTime now = LocalDateTime.now();

        // 1. DEADLINE으로 변경될 대상 ID 조회 (상태가 RUNNING인 것만)
        List<Long> toDeadlineIds = auctionRepository.findRunningAuctionsToDeadline(now);

        if (toDeadlineIds.isEmpty()) return;

        // 2. DB 상태 변경 (RUNNING -> DEADLINE)
        auctionRepository.markDeadlineAuctions(now);

        // 3. 알림 발송 (확보된 ID 리스트 기반)
        for (Long auctionId : toDeadlineIds) {
            auctionRedisService.setRedis(auctionId);
            auctionNotificationService.notifyImminent(auctionId);
        }
    }

    /**
     * DEADLINE -> ENDED
     */
    @Transactional
    public int end(LocalDateTime now) {
        List<Long> toEndIds = auctionRepository.findIdsToEnd(AuctionStatus.DEADLINE, now);

        int updated = auctionRepository.endRunningAuctions(
                AuctionStatus.DEADLINE,
                AuctionStatus.ENDED,
                now
        );

        // 종료 이벤트 발송
        for (Long auctionId : toEndIds) {
            try {
                auctionRedisService.setRedis(auctionId);
                // 스냅샷 기반으로 종료 payload 구성 (DB 접근 최소화)
                var snap = snapshotService.getSnapshot(auctionId);

                AuctionEndedPayload payload = AuctionEndedPayload.builder()
                        .auctionId(auctionId)
                        .winnerUserId(snap.getCurrentBidderId()) // 유찰이면 null일 수 있음
                        .finalPrice(snap.getCurrentPrice())
                        .bidCount(snap.getBidCount())
                        .endedAt(now)
                        .message(snap.getCurrentBidderId() == null ? 
                                NotificationConstants.MSG_WS_AUCTION_ENDED_FAILED : 
                                NotificationConstants.MSG_WS_AUCTION_ENDED_SUCCESS)
                        .build();

                auctionMessageService.sendAuctionEnded(auctionId, payload);

            } catch (Exception e) {
                log.error("Failed to send AUCTION_ENDED for auctionId={}", auctionId, e);
            }
        }

        return updated;
    }

    /**
     * ended -> success or failed
     */

    @Transactional
    public void result() {
        // ENDED 상태인 경매를 페이지 단위로 가져옴
        Page<Auction> endedPage = auctionRepository.findAllByStatus(
                AuctionStatus.ENDED,
                PageRequest.of(0, 100)
        );

        List<Auction> endedAuctions = endedPage.getContent();

        for (Auction auction : endedAuctions) {
            Long auctionId = auction.getId();
            try {
                processAuctionResult(auction);
            } catch (Exception e) {
                // 한 건 실패해도 다음 경매 계속 처리
                log.error("Failed to finalize result for auctionId={}", auctionId, e);
            }
        }
    }

    private void processAuctionResult(Auction auction) {
        Long auctionId = auction.getId();
        Optional<Bid> winningBid = bidRepository.findFirstByAuctionIdOrderByBidPriceDesc(auctionId);

        if (winningBid.isPresent()) {
            Bid winnerBid = winningBid.get();

            // 1) ENDED -> SUCCESS 선점
            int changed = auctionRepository.finalizeAuctionStatus(
                    auctionId,
                    AuctionStatus.ENDED,
                    AuctionStatus.SUCCESS
            );

            // 2) 선점 성공한 경우에만 주문 생성 + 이벤트 발송
            if (changed == 1) {
                auctionRedisService.setRedis(auctionId);
                // 주문 생성
                orderService.createOrder(auction, winnerBid.getBidder(), winnerBid.getBidPrice());

                // 알림 발송 (낙찰자, 패찰자, 판매자)
                sendSuccessNotifications(auction, winnerBid);

                // WS 메시지 발송
                sendResultPayload(auctionId, AuctionStatus.SUCCESS, winnerBid.getBidder().getId());
            } else {
                log.debug("[AuctionResult] skip duplicated success finalize auctionId={}", auctionId);
            }

        } else {
            // 유찰 : failed
            // 1) ENDED -> FAILED 선점
            int changed = auctionRepository.finalizeAuctionStatus(
                    auctionId,
                    AuctionStatus.ENDED,
                    AuctionStatus.FAILED
            );

            // 2) 선점 성공한 경우에만 이벤트 발송
            if (changed == 1) {
                auctionRedisService.setRedis(auctionId);
                // 알림 발송 (판매자)
                sendFailureNotifications(auction);

                // WS 메시지 발송
                sendResultPayload(auctionId, AuctionStatus.FAILED, null);
            } else {
                log.debug("[AuctionResult] skip duplicated failed finalize auctionId={}", auctionId);
            }
        }
    }

    private void sendSuccessNotifications(Auction auction, Bid winnerBid) {
        String itemTitle = auction.getItem().getTitle();

        // 1. 모든 참여자 조회 (중복 제거)
        List<User> participants = bidRepository.findDistinctBiddersByAuctionId(auction.getId());

        for (User participant : participants) {
            if (participant.getId().equals(winnerBid.getBidder().getId())) {
                // 1-1. 낙찰자에게 성공 알림 (PURCHASED)
                auctionNotificationService.sendNotification(
                        participant.getId(),
                        NotificationType.PURCHASED,
                        String.format(NotificationConstants.MSG_AUCTION_WINNER, itemTitle),
                        NotificationConstants.REF_TYPE_AUCTION,
                        auction.getId()
                );
            }
            else {
                // 1-2. 패찰자(나머지 참여자)에게 실패 알림 (NO_PURCHASE)
                auctionNotificationService.sendNotification(
                        participant.getId(),
                        NotificationType.NO_PURCHASE,
                        String.format(NotificationConstants.MSG_AUCTION_LOSER, itemTitle),
                        NotificationConstants.REF_TYPE_AUCTION,
                        auction.getId()
                );
            }
        }

        // 2. 판매자에게 판매 완료 알림 (PURCHASED)
        auctionNotificationService.sendNotification(
                auction.getSeller().getId(),
                NotificationType.PURCHASED,
                String.format(NotificationConstants.MSG_AUCTION_SOLD, itemTitle),
                NotificationConstants.REF_TYPE_AUCTION,
                auction.getId()
        );
    }

    private void sendFailureNotifications(Auction auction) {
        String itemTitle = auction.getItem().getTitle();
        // 3. 판매자에게 유찰 알림 (NO_PURCHASE)
        auctionNotificationService.sendNotification(
                auction.getSeller().getId(),
                NotificationType.NO_PURCHASE,
                String.format(NotificationConstants.MSG_AUCTION_FAILED, itemTitle),
                NotificationConstants.REF_TYPE_AUCTION,
                auction.getId()
        );
    }

    private void sendResultPayload(Long auctionId, AuctionStatus result, Long winnerId) {
        var snap = snapshotService.getSnapshot(auctionId);

        AuctionResultPayload payload = AuctionResultPayload.builder()
                .auctionId(auctionId)
                .result(result.name())
                .winnerUserId(winnerId)
                .finalPrice(result == AuctionStatus.FAILED ? null : snap.getCurrentPrice())
                .bidCount(snap.getBidCount())
                .decidedAt(LocalDateTime.now())
                .build();

        auctionMessageService.sendAuctionResult(auctionId, payload);
    }

    /**
     * (핫딜) READY -> RUNNING (startAt 기준)
     */
    @Transactional
    public int exposeHotDeals(LocalDateTime now) {
        List<Long> ids = auctionRepository.findHotDealIdsToRun(AuctionStatus.READY, now);
        if (ids.isEmpty()) return 0;

        int updated = auctionRepository.runHotDeals(AuctionStatus.READY, AuctionStatus.RUNNING, now);
        if (updated <= 0) return 0;

        for (Long auctionId : ids) {
            auctionRedisService.setRedis(auctionId);
        }
        return updated;
    }
}
