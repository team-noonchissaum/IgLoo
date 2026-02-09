package noonchissaum.backend.global.config;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.statistics.batch.DailyStatisticsTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 스프링 배치 설정
 * Job과 Step을 정의하고 연결
 */
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final DailyStatisticsTasklet dailyStatisticsTasklet;

    /**
     * Job 정의 - 배치 작업의 최상위 단위
     * 실행 이력이 BATCH_JOB_INSTANCE, BATCH_JOB_EXECUTION 테이블에 기록됨
     */
    @Bean
    public Job dailyStatisticsJob(JobRepository jobRepository, Step dailyStatisticsStep) {
        return new JobBuilder("dailyStatisticsJob", jobRepository)
                .start(dailyStatisticsStep)
                .build();
    }

    /**
     * Step 정의 - Job 안에서 실제 작업을 수행하는 단위
     * transactionManager로 트랜잭션 묶음 (에러 시 롤백)
     */
    @Bean
    public Step dailyStatisticsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("dailyStatisticsStep", jobRepository)
                .tasklet(dailyStatisticsTasklet, transactionManager)
                .build();
    }
}
