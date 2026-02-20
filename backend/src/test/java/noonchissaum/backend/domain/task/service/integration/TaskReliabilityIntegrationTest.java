package noonchissaum.backend.domain.task.service.integration;

import noonchissaum.backend.domain.auction.service.BidRecordService;
import noonchissaum.backend.domain.auction.service.BidService;
import noonchissaum.backend.domain.task.dto.DbUpdateEvent;
import noonchissaum.backend.domain.task.entity.AsyncTask;
import noonchissaum.backend.domain.task.repository.AsyncTaskRepository;
import noonchissaum.backend.domain.task.service.AsyncTaskTxService;
import noonchissaum.backend.domain.task.service.DbEventListener;
import noonchissaum.backend.domain.wallet.service.WalletRecordService;
import noonchissaum.backend.global.RedisKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Executor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.task.scheduling.enabled=false"
})
@Tag("integration")
class TaskReliabilityIntegrationTest {

    @Autowired
    private AsyncTaskRepository asyncTaskRepository;
    @Autowired
    private AsyncTaskTxService asyncTaskTxService;
    @Autowired
    private DbEventListener dbEventListener;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private OuterTxProbe outerTxProbe;

    @MockitoBean
    private BidService bidService;
    @MockitoBean
    private BidRecordService bidRecordService;
    @MockitoBean
    private WalletRecordService walletRecordService;

    @BeforeEach
    void setUp() {
        asyncTaskRepository.deleteAll();
    }

    @Test
    @DisplayName("외부 트랜잭션 롤백 시에도 REQUIRES_NEW task row는 유지")
    void requiresNewTask_survivesOuterRollback() {
        String requestId = "task-rn-" + UUID.randomUUID();
        DbUpdateEvent event = new DbUpdateEvent(11L, -1L, BigDecimal.valueOf(10000), BigDecimal.ZERO, 101L, requestId);

        assertThrows(RuntimeException.class, () -> outerTxProbe.startTaskAndRollback(event));

        AsyncTask task = asyncTaskRepository.findByRequestId(requestId).orElseThrow();
        assertThat(readIsSuccess(task)).isFalse();
    }

    @Test
    @DisplayName("DB 반영 실패 시 pending 키 유지 및 task 성공 마킹 미적용")
    void pendingKey_remainsWhenDbUpdateFails() {
        String requestId = "task-fail-" + UUID.randomUUID();
        DbUpdateEvent event = new DbUpdateEvent(21L, -1L, BigDecimal.valueOf(12000), BigDecimal.ZERO, 201L, requestId);
        String pendingKey = RedisKeys.pendingUser(21L);
        redisTemplate.opsForSet().add(pendingKey, requestId);

        when(bidService.isExistRequestId(requestId)).thenReturn(true);
        doThrow(new RuntimeException("db write failed"))
                .when(walletRecordService)
                .saveWalletRecord(21L, BigDecimal.valueOf(12000), -1L, BigDecimal.ZERO, 201L);

        dbEventListener.handleWalletUpdate(event);

        AsyncTask task = asyncTaskRepository.findByRequestId(requestId).orElseThrow();
        assertThat(readIsSuccess(task)).isFalse();
        assertThat(redisTemplate.opsForSet().isMember(pendingKey, requestId)).isTrue();

        redisTemplate.delete(pendingKey);
    }

    @Test
    @DisplayName("DB 반영 성공 시 afterCommit 이후 pending 키 제거 및 task 성공 마킹")
    void pendingKey_removedWhenDbUpdateCommits() {
        String requestId = "task-ok-" + UUID.randomUUID();
        DbUpdateEvent event = new DbUpdateEvent(31L, -1L, BigDecimal.valueOf(13000), BigDecimal.ZERO, 301L, requestId);
        String pendingKey = RedisKeys.pendingUser(31L);
        redisTemplate.opsForSet().add(pendingKey, requestId);

        when(bidService.isExistRequestId(requestId)).thenReturn(true);

        dbEventListener.handleWalletUpdate(event);

        AsyncTask task = asyncTaskRepository.findByRequestId(requestId).orElseThrow();
        assertThat(readIsSuccess(task)).isTrue();
        assertThat(redisTemplate.opsForSet().isMember(pendingKey, requestId)).isFalse();
    }

    private boolean readIsSuccess(AsyncTask task) {
        Object value = org.springframework.test.util.ReflectionTestUtils.getField(task, "isSuccess");
        return Boolean.TRUE.equals(value);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean(name = "DBTaskExcutor")
        Executor dbTaskExcutor() {
            // @Async 메서드를 동일 스레드에서 실행해 테스트를 결정적으로 만든다.
            return new SyncTaskExecutor();
        }

        @Bean
        OuterTxProbe outerTxProbe(AsyncTaskTxService asyncTaskTxService) {
            return new OuterTxProbe(asyncTaskTxService);
        }
    }

    static class OuterTxProbe {
        private final AsyncTaskTxService asyncTaskTxService;

        OuterTxProbe(AsyncTaskTxService asyncTaskTxService) {
            this.asyncTaskTxService = asyncTaskTxService;
        }

        @Transactional
        public void startTaskAndRollback(DbUpdateEvent event) {
            asyncTaskTxService.startTask(event);
            throw new RuntimeException("force rollback");
        }
    }
}
