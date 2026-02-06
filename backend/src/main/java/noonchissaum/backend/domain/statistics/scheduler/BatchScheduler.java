package noonchissaum.backend.domain.statistics.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 배치 스케줄러
 * 정해진 시간에 배치 Job을 실행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchScheduler {

    /** Job을 실행시키는 역할 */
    private final JobLauncher jobLauncher;

    /** 실행할 Job */
    private final Job dailyStatisticsJob;

    /**
     * 매일 자정(00:00:00)에 실행
     * 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void runDailyStatisticsJob() {
        try {
            // 매번 다른 파라미터로 새로운 실행으로 인식시킴
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(dailyStatisticsJob, params);
            log.info("[스케줄러] 일일 통계 배치 실행 완료");

        } catch (Exception e) {
            log.error("[스케줄러] 일일 통계 배치 실행 실패", e);
        }
    }
}
