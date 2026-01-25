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
@Component //컴포넌트 필요하다고 합니다!.. 보광님 죄송합니다....
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
     * 1분마다 한번씩 조회하여
     */
    @Scheduled(fixedDelay = 60_000 )
    @Transactional
    public void end() {
        LocalDateTime now = LocalDateTime.now();

        int updateEnd = auctionRepository.endRunningAuctions(
                AuctionStatus.RUNNING,
                AuctionStatus.ENDED,
                now
        );
        if(updateEnd > 0){
            log.info("[AuctionEnd] ended={}", updateEnd);
        }
    }
}
