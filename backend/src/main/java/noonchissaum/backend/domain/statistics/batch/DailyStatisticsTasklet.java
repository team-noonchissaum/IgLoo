package noonchissaum.backend.domain.statistics.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.statistics.entity.DailyStatistics;
import noonchissaum.backend.domain.statistics.repository.DailyStatisticsRepository;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일일 통계 집계 Tasklet
 * 매일 자정에 전일(어제) 데이터를 집계해서 daily_statistics 테이블에 저장
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyStatisticsTasklet implements Tasklet {

    private final DailyStatisticsRepository dailyStatisticsRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        // 집계 대상: 어제 날짜
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 중복 실행 방지 - 이미 있으면 스킵
        if (dailyStatisticsRepository.existsByStatDate(yesterday)) {
            log.info("[배치] {} 통계 이미 존재 → 스킵", yesterday);
            return RepeatStatus.FINISHED;
        }

        // 어제 하루 범위 (00:00:00 ~ 23:59:59)
        LocalDateTime startOfDay = yesterday.atStartOfDay();
        LocalDateTime endOfDay = yesterday.plusDays(1).atStartOfDay();

        /* ===== 경매 통계 집계 ===== */
        int auctionTotalCount = (int) dailyStatisticsRepository.countAuctionsBetween(startOfDay, endOfDay);
        int auctionSuccessCount = (int) dailyStatisticsRepository.countSuccessAuctionsBetween(startOfDay, endOfDay);
        int auctionFailCount = (int) dailyStatisticsRepository.countFailedAuctionsBetween(startOfDay, endOfDay);
        int auctionBlockedCount = (int) dailyStatisticsRepository.countBlockedAuctionsBetween(startOfDay, endOfDay);

        /* ===== 거래 통계 집계 ===== */
        long completedOrderCount = dailyStatisticsRepository.countCompletedOrdersBetween(startOfDay, endOfDay);
        long canceledOrderCount = dailyStatisticsRepository.countCanceledOrdersBetween(startOfDay, endOfDay);

        /* ===== 크레딧 통계 집계 ===== */
        BigDecimal chargeAmount = dailyStatisticsRepository.sumAmountByTypeBetween(TransactionType.CHARGE, startOfDay, endOfDay);
        BigDecimal withdrawAmount = dailyStatisticsRepository.sumAmountByTypeBetween(TransactionType.WITHDRAW_CONFIRM, startOfDay, endOfDay);
        BigDecimal depositForfeitAmount = dailyStatisticsRepository.sumAmountByTypeBetween(TransactionType.DEPOSIT_FORFEIT, startOfDay, endOfDay);
        BigDecimal depositReturnAmount = dailyStatisticsRepository.sumAmountByTypeBetween(TransactionType.DEPOSIT_RETURN, startOfDay, endOfDay);
        BigDecimal settlementAmount = dailyStatisticsRepository.sumAmountByTypeBetween(TransactionType.SETTLEMENT_IN, startOfDay, endOfDay);

        DailyStatistics stat = DailyStatistics.builder()
                .statDate(yesterday)
                .auctionTotalCount(auctionTotalCount)
                .auctionSuccessCount(auctionSuccessCount)
                .auctionFailCount(auctionFailCount)
                .auctionBlockedCount(auctionBlockedCount)
                .completedOrderCount(completedOrderCount)
                .canceledOrderCount(canceledOrderCount)
                .chargeAmount(chargeAmount)
                .withdrawAmount(withdrawAmount)
                .depositForfeitAmount(depositForfeitAmount)
                .depositReturnAmount(depositReturnAmount)
                .settlementAmount(settlementAmount)
                .build();

        dailyStatisticsRepository.save(stat);
        log.info("[배치] {} 일일 통계 저장 완료", yesterday);

        return RepeatStatus.FINISHED;
    }
}
