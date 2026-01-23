package noonchissaum.backend.domain.auction.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class AuctionExposureScheduler {
    private final AuctionRepository auctionRepository;

    /**
     * 5분뒤 상태 RUNNING으로 전환
     */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void expose() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusMinutes(5);

        int updated = auctionRepository.exposeReadyAuctions(
                AuctionStatus.READY,
                AuctionStatus.RUNNING,
                threshold,
                now
        );
        if(updated > 0){
            log.info("[AuctionExpose] running={}", updated);
        }
    }



    /**
     * RUNNING상태에서 ENDED로 전환
     */
    @Scheduled(fixedRate = )
    @Transactional
    public void end() {
       LocalDateTime now = LocalDateTime.now();
       auctionRepository.findByStartAt()

    }


    /**
     * RUNNING상태에서 CANCELLED로 전환
     */

}
