package noonchissaum.backend.domain.auction.service.integration;

import noonchissaum.backend.domain.auction.service.BidRecordService;
import noonchissaum.backend.domain.auction.service.BidService;
import noonchissaum.backend.domain.task.dto.DbUpdateEvent;
import noonchissaum.backend.domain.task.entity.AsyncTask;
import noonchissaum.backend.domain.task.repository.AsyncTaskRepository;
import noonchissaum.backend.domain.task.service.DbEventListener;
import noonchissaum.backend.domain.wallet.service.WalletRecordService;
import noonchissaum.backend.global.RedisKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.task.scheduling.enabled=false"
})
@Tag("integration")
class BidAsyncConsistencyIntegrationTest {

    @Autowired
    private DbEventListener dbEventListener;
    @Autowired
    private AsyncTaskRepository asyncTaskRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;

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
    @DisplayName("입찰 비동기 반영 성공 시 task 성공 마킹 및 신규/이전 입찰자 pending 제거")
    void bidAsyncSuccess_marksTaskSuccess_andRemovesBothPendingKeys() {
        String requestId = "bid-async-ok-" + UUID.randomUUID();
        DbUpdateEvent event = new DbUpdateEvent(
                101L,
                202L,
                BigDecimal.valueOf(12000),
                BigDecimal.valueOf(11000),
                999L,
                requestId
        );
        String newBidderPending = RedisKeys.pendingUser(101L);
        String prevBidderPending = RedisKeys.pendingUser(202L);
        redisTemplate.opsForSet().add(newBidderPending, requestId);
        redisTemplate.opsForSet().add(prevBidderPending, requestId);

        when(bidService.isExistRequestId(requestId)).thenReturn(false);

        dbEventListener.handleWalletUpdate(event);

        AsyncTask task = asyncTaskRepository.findByRequestId(requestId).orElseThrow();
        assertThat(readIsSuccess(task)).isTrue();
        assertThat(redisTemplate.opsForSet().isMember(newBidderPending, requestId)).isFalse();
        assertThat(redisTemplate.opsForSet().isMember(prevBidderPending, requestId)).isFalse();

        verify(bidRecordService).saveBidRecord(999L, 101L, BigDecimal.valueOf(12000), requestId);
        verify(walletRecordService).saveWalletRecord(101L, BigDecimal.valueOf(12000), 202L, BigDecimal.valueOf(11000), 999L);
    }

    @Test
    @DisplayName("입찰 비동기 반영 실패 시 task 미성공 상태 유지 및 신규/이전 입찰자 pending 유지")
    void bidAsyncFailure_keepsTaskIncomplete_andPreservesBothPendingKeys() {
        String requestId = "bid-async-fail-" + UUID.randomUUID();
        DbUpdateEvent event = new DbUpdateEvent(
                303L,
                404L,
                BigDecimal.valueOf(15000),
                BigDecimal.valueOf(13000),
                1000L,
                requestId
        );
        String newBidderPending = RedisKeys.pendingUser(303L);
        String prevBidderPending = RedisKeys.pendingUser(404L);
        redisTemplate.opsForSet().add(newBidderPending, requestId);
        redisTemplate.opsForSet().add(prevBidderPending, requestId);

        when(bidService.isExistRequestId(requestId)).thenReturn(false);
        doThrow(new RuntimeException("bid save failed"))
                .when(bidRecordService)
                .saveBidRecord(1000L, 303L, BigDecimal.valueOf(15000), requestId);

        dbEventListener.handleWalletUpdate(event);

        AsyncTask task = asyncTaskRepository.findByRequestId(requestId).orElseThrow();
        assertThat(readIsSuccess(task)).isFalse();
        assertThat(redisTemplate.opsForSet().isMember(newBidderPending, requestId)).isTrue();
        assertThat(redisTemplate.opsForSet().isMember(prevBidderPending, requestId)).isTrue();
    }

    private boolean readIsSuccess(AsyncTask task) {
        Object value = org.springframework.test.util.ReflectionTestUtils.getField(task, "isSuccess");
        return Boolean.TRUE.equals(value);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean(name = "DBTaskExcutor")
        Executor dbTaskExcutor() {
            return new SyncTaskExecutor();
        }
    }
}
