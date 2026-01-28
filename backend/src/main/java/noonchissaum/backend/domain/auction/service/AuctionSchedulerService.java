package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuctionSchedulerService {

    private final AuctionRepository auctionRepository;
    private final AuctionRedisService auctionRedisService;

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

        // 종료된 경매들의 낙찰/유찰 후처리를 붙이기 가능

        return updated;
    }
}
