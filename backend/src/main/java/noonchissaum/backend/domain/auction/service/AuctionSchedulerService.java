package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
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
        auctionRepository.markDeadlineAuctions(LocalDateTime.now());
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
                // 주문생성
                orderService.createOrder(auction, winningBid.get().getBidder());
                auctionRepository.finalizeAuctionStatus(auction.getId(), AuctionStatus.ENDED,AuctionStatus.SUCCESS);

            } else {
                //유찰 : failed
                auctionRepository.finalizeAuctionStatus(auction.getId(), AuctionStatus.ENDED,AuctionStatus.FAILED);
            }
        }

    }

}
