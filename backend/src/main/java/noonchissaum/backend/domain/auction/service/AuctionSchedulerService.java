package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        long startMs = System.currentTimeMillis();
        int updated = auctionRepository.endRunningAuctions(
                AuctionStatus.DEADLINE,
                AuctionStatus.ENDED,
                now
        );
        long elapsed = System.currentTimeMillis() - startMs;

        if (updated > 0) {
            log.info("[AuctionEnd] ended={} elapsedMs={}", updated, elapsed);
        } else {
            log.debug("[AuctionEnd] ended=0 elapsedMs={}", elapsed);
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

        // 최고 입찰자 조회
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
            }
        }

    }

}
