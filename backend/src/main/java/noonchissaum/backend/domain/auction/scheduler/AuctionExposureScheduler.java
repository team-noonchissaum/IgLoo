package noonchissaum.backend.domain.auction.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.service.AuctionSchedulerService;
import noonchissaum.backend.domain.wallet.service.WalletService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class AuctionExposureScheduler {
    private final AuctionSchedulerService auctionSchedulerService;

    /**
     * 진행 중인 모든 경매의 실시간 정보를 1초마다 중계합니다.
     */
    @Scheduled(fixedRate = 30_000)
    public void broadcastAuctions() {
        auctionSchedulerService.broadcastActiveAuctions();
    }

    /**
     * 5분뒤 READY상태에서 RUNNING으로 전환
     */
    @Scheduled(fixedRate = 60_000)
    public void exposeReadyAuctions() {
        LocalDateTime now = LocalDateTime.now();
        auctionSchedulerService.expose(now);
    }

    /**
     * RUNNING상태에서 DEADLINE으로 전환
     * 1분마다 한번씩 조회하여
     */
    @Scheduled(fixedRate = 60_000)
    public void exposeAuctions() {
        auctionSchedulerService.markDeadline();
    }

    /**
     * DEADLINE 상태에서 end으로 전환
     * 1분마다 한번씩 조회하여
     */
    @Scheduled(fixedRate = 60_000)
    public void endRunningAuctions() {
        LocalDateTime now = LocalDateTime.now();
        auctionSchedulerService.end(now);
    }

    /**
     * end상태에서 SUCCESS or failed로 전환
     * 1분마다 조회
     */
    @Scheduled(fixedRate = 60_000)
    public void resultAuctions() {
//        LocalDateTime now = LocalDateTime.now();
        auctionSchedulerService.result();
    }

}

