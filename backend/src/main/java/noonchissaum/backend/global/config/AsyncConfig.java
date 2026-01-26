package noonchissaum.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "walletTaskExcutor") // 에러 메시지에 나온 이름과 정확히 일치시켜야 합니다.
    public Executor walletTaskExcutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);     // 기본적으로 유지할 스레드 수
        executor.setMaxPoolSize(20);      // 요청이 많아질 때 최대 스레드 수
        executor.setQueueCapacity(100);   // 대기 큐 크기 (100명 테스트니까 100 이상 추천)
        executor.setThreadNamePrefix("WalletAsync-");
        executor.initialize();
        return executor;
    }
}
