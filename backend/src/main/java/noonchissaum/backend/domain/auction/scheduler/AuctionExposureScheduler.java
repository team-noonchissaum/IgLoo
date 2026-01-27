package noonchissaum.backend.domain.auction.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.service.AuctionSchedulerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class AuctionExposureScheduler {
    private final AuctionSchedulerService auctionSchedulerService;

    /**
     * 5분뒤 상태 RUNNING으로 전환
     */
    @Scheduled(fixedRate = 300_000)
    public void exposeReadyAuctions() {
        LocalDateTime now = LocalDateTime.now();
        auctionSchedulerService.expose(now);
    }

    /**
     * RUNNING상태에서 DEADLINE으로 전환
     * 1분마다 한번씩 조회하여
     */
    @Scheduled(fixedRate = 60_000)
    public void endRunningAuctions() {
        LocalDateTime now = LocalDateTime.now();
        auctionSchedulerService.end(now);
    }

    /**
     * 1분마다 조회하여 imminent_minutes 보다 적은 시간을 가지고 있다면
     * 상태값 변경
     */
    @Scheduled(fixedRate = 60_000)
    public void exposeAuctions() {
        auctionSchedulerService.markDeadLine();
    }
}

