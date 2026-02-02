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
        // ENDED 상태인 경매를 페이지 단위로 가져옴 (필요하면 while로 페이지 반복 가능)
        Page<Auction> endedPage = auctionRepository.findAllByStatus(
                AuctionStatus.ENDED,
                PageRequest.of(0, 100)
        );

        List<Auction> endedAuctions = endedPage.getContent();

        for (Auction auction : endedAuctions) {
            Long auctionId = auction.getId();

            try {
                // 1) 최고 입찰자 조회 (한 번만)
                Optional<Bid> winningBidOpt =
                        bidRepository.findFirstByAuctionIdOrderByBidPriceDesc(auctionId);

                String itemTitle = auction.getItem().getTitle();

                if (winningBidOpt.isPresent()) {
                    // ===== 낙찰(SUCCESS) 케이스 =====
                    Bid winnerBid = winningBidOpt.get();

                    // 2) ENDED -> SUCCESS 선점 (중복 처리 방지 핵심)
                    int changed = auctionRepository.finalizeAuctionStatus(
                            auctionId,
                            AuctionStatus.ENDED,
                            AuctionStatus.SUCCESS
                    );

                    if (changed != 1) {
                        log.debug("[AuctionResult] skip duplicated success finalize auctionId={}", auctionId);
                        continue;
                    }

                    // 3) 선점 성공한 경우에만 주문 생성
                    orderService.createOrder(auction, winnerBid.getBidder());

                    // 4) WS 결과 이벤트 발송
                    var snap = snapshotService.getSnapshot(auctionId);

                    AuctionResultPayload wsPayload = AuctionResultPayload.builder()
                            .auctionId(auctionId)
                            .result(AuctionStatus.SUCCESS.name())
                            .winnerUserId(winnerBid.getBidder().getId())
                            .finalPrice(snap.getCurrentPrice())
                            .bidCount(snap.getBidCount())
                            .decidedAt(LocalDateTime.now())
                            .build();

                    auctionMessageService.sendAuctionResult(auctionId, wsPayload);

                    // 5) 알림 발송
                    // 5-1) 참여자(입찰자) 조회 (중복 제거)
                    List<noonchissaum.backend.domain.user.entity.User> participants =
                            bidRepository.findDistinctBiddersByAuctionId(auctionId);

                    for (noonchissaum.backend.domain.user.entity.User participant : participants) {
                        if (participant.getId().equals(winnerBid.getBidder().getId())) {
                            // 낙찰자 알림 (PURCHASED)
                            auctionNotificationService.sendNotification(
                                    participant.getId(),
                                    NotificationType.PURCHASED,
                                    "축하합니다! '" + itemTitle + "' 경매에 낙찰되었습니다.",
                                    "AUCTION",
                                    auctionId
                            );
                        } else {
                            // 패찰자 알림 (NO_PURCHASE)
                            auctionNotificationService.sendNotification(
                                    participant.getId(),
                                    NotificationType.NO_PURCHASE,
                                    "아쉽게도 '" + itemTitle + "' 경매 낙찰에 실패했습니다.",
                                    "AUCTION",
                                    auctionId
                            );
                        }
                    }

                    // 5-2) 판매자 알림 (판매 완료)
                    auctionNotificationService.sendNotification(
                            auctionId,
                            NotificationType.PURCHASED,
                            "등록하신 '" + itemTitle + "' 물품이 판매되었습니다.",
                            "AUCTION",
                            auctionId
                    );

                } else {
                    // ===== 유찰(FAILED) 케이스 =====

                    // 2) ENDED -> FAILED 선점
                    int changed = auctionRepository.finalizeAuctionStatus(
                            auctionId,
                            AuctionStatus.ENDED,
                            AuctionStatus.FAILED
                    );

                    if (changed != 1) {
                        log.debug("[AuctionResult] skip duplicated failed finalize auctionId={}", auctionId);
                        continue;
                    }

                    // 3) WS 결과 이벤트 발송 (유찰)
                    var snap = snapshotService.getSnapshot(auctionId);

                    AuctionResultPayload wsPayload = AuctionResultPayload.builder()
                            .auctionId(auctionId)
                            .result(AuctionStatus.FAILED.name())
                            .winnerUserId(null)
                            .finalPrice(null)
                            .bidCount(snap.getBidCount())
                            .decidedAt(LocalDateTime.now())
                            .build();

                    auctionMessageService.sendAuctionResult(auctionId, wsPayload);

                    // 4) 판매자 유찰 알림 (NO_PURCHASE)
                    auctionNotificationService.sendNotification(
                            auctionId,
                            NotificationType.NO_PURCHASE,
                            "아쉽게도 '" + itemTitle + "' 경매가 유찰되었습니다.",
                            "AUCTION",
                            auctionId
                    );
                }

            } catch (Exception e) {
                // 한 건 실패해도 다음 경매 계속 처리
                log.error("Failed to finalize result for auctionId={}", auctionId, e);
                }
         }
        }
    }