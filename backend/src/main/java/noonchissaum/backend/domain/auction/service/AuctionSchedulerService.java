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
import noonchissaum.backend.domain.notification.entity.NotificationType;
import noonchissaum.backend.domain.notification.service.AuctionNotificationService;
import noonchissaum.backend.domain.notification.service.NotificationService;
import noonchissaum.backend.domain.order.service.OrderService;
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
    private final NotificationService notificationService;
    private final AuctionNotificationService auctionNotificationService;

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
        int updated = auctionRepository.exposeReadyAuctions(
                AuctionStatus.READY,
                AuctionStatus.RUNNING,
                threshold,
                now
        );
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
    public void markDeadLine() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> toDeadlineIds = auctionRepository.findRunningAuctionsToDeadline(now);

        auctionRepository.markDeadlineAuctions(now);

        for (Long auctionId : toDeadlineIds) {
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
                // 스냅샷 기반으로 종료 payload 구성 (DB 접근 최소화)
                var snap = snapshotService.getSnapshot(auctionId);

                AuctionEndedPayload payload = AuctionEndedPayload.builder()
                        .auctionId(auctionId)
                        .winnerUserId(snap.getCurrentBidderId()) // 유찰이면 null일 수 있음
                        .finalPrice(snap.getCurrentPrice())
                        .bidCount(snap.getBidCount())
                        .endedAt(now)
                        .message(snap.getCurrentBidderId() == null ? "유찰되었습니다." : "경매가 종료되었습니다.")
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
                PageRequest.of(0,100)
        );

        List<Auction> endedAuctions = endedPage.getContent();


        for (Auction auction : endedAuctions) {
            Optional<Bid> winningBid = bidRepository.findFirstByAuctionIdOrderByBidPriceDesc(auction.getId());
            
            if (winningBid.isPresent()) {
                //낙찰 성공: 주문생성 + SUCCESS 변경
                Bid winnerBid = winningBid.get();
                // 주문생성
                orderService.createOrder(auction, winnerBid.getBidder());
                auctionRepository.finalizeAuctionStatus(auction.getId(), AuctionStatus.ENDED,AuctionStatus.SUCCESS);
                
                //알림 발송
                String itemTitle = auction.getItem().getTitle();

                // 1. 모든 참여자 조회 (중복 제거)
                List<noonchissaum.backend.domain.user.entity.User> participants = bidRepository.findDistinctBiddersByAuctionId(auction.getId());
                
                for (noonchissaum.backend.domain.user.entity.User participant : participants) {
                    if (participant.getId().equals(winnerBid.getBidder().getId())) {
                        // 1-1. 낙찰자에게 성공 알림 (PURCHASED)
                        notificationService.send(
                                participant,
                                NotificationType.PURCHASED,
                                "축하합니다! '" + itemTitle + "' 경매에 낙찰되었습니다.",
                                "AUCTION",
                                auction.getId()
                        );
                    } else {
                        // 1-2. 패찰자(나머지 참여자)에게 실패 알림 (NO_PURCHASE)
                        notificationService.send(
                                participant,
                                NotificationType.NO_PURCHASE,
                                "아쉽게도 '" + itemTitle + "' 경매 낙찰에 실패했습니다.",
                                "AUCTION",
                                auction.getId()
                        );
                    }
                }

                // 2. 판매자에게 판매 완료 알림 (PURCHASED)
                notificationService.send(
                        auction.getSeller(),
                        NotificationType.PURCHASED,
                        "등록하신 '" + itemTitle + "' 물품이 판매되었습니다.",
                        "AUCTION",
                        auction.getId()
                );

            } else {
                //유찰 : failed
                auctionRepository.finalizeAuctionStatus(auction.getId(), AuctionStatus.ENDED,AuctionStatus.FAILED);
                
                // 3. 판매자에게 유찰 알림 (NO_PURCHASE)
                notificationService.send(
                        auction.getSeller(),
                        NotificationType.NO_PURCHASE,
                        "아쉽게도 '" + auction.getItem().getTitle() + "' 경매가 유찰되었습니다.",
                        "AUCTION",
                        auction.getId()
                );
            Long auctionId = auction.getId();

            try {
                Optional<Bid> winningBid =
                        bidRepository.findFirstByAuctionIdOrderByBidPriceDesc(auctionId);

                if (winningBid.isPresent()) {
                    Bid win = winningBid.get();

                    // 1) ENDED -> SUCCESS 선점
                    int changed = auctionRepository.finalizeAuctionStatus(
                            auctionId,
                            AuctionStatus.ENDED,
                            AuctionStatus.SUCCESS
                    );

                    // 2) 선점 성공한 경우에만 주문 생성 + 이벤트 발송
                    if (changed == 1) {
                        // 주문 생성 (중복 방지 완료)
                        orderService.createOrder(auction, win.getBidder());

                        // 스냅샷 기반 payload 구성
                        var snap = snapshotService.getSnapshot(auctionId);

                        AuctionResultPayload payload = AuctionResultPayload.builder()
                                .auctionId(auctionId)
                                .result(AuctionStatus.SUCCESS.name())
                                .winnerUserId(win.getBidder().getId())
                                .finalPrice(snap.getCurrentPrice())
                                .bidCount(snap.getBidCount())
                                .decidedAt(LocalDateTime.now())
                                .build();

                        auctionMessageService.sendAuctionResult(auctionId, payload);
                    } else {

                        log.debug("[AuctionResult] skip duplicated success finalize auctionId={}", auctionId);
                    }

                } else {
                    //  1) ENDED -> FAILED 선점
                    int changed = auctionRepository.finalizeAuctionStatus(
                            auctionId,
                            AuctionStatus.ENDED,
                            AuctionStatus.FAILED
                    );

                    //  2) 선점 성공한 경우에만 이벤트 발송
                    if (changed == 1) {
                        var snap = snapshotService.getSnapshot(auctionId);

                        AuctionResultPayload payload = AuctionResultPayload.builder()
                                .auctionId(auctionId)
                                .result(AuctionStatus.FAILED.name())
                                .winnerUserId(null)
                                .finalPrice(null)
                                .bidCount(snap.getBidCount())
                                .decidedAt(LocalDateTime.now())
                                .build();

                        auctionMessageService.sendAuctionResult(auctionId, payload);
                    } else {
                        log.debug("[AuctionResult] skip duplicated failed finalize auctionId={}", auctionId);
                    }
                }

            } catch (Exception e) {
                // 한 건 실패해도 다음 경매 계속 처리
                log.error("Failed to finalize result for auctionId={}", auctionId, e);
            }
        }



    }


}
