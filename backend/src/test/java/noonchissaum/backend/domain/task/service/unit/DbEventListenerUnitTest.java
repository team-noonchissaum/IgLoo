package noonchissaum.backend.domain.task.service.unit;

import noonchissaum.backend.domain.auction.service.BidRecordService;
import noonchissaum.backend.domain.auction.service.BidService;
import noonchissaum.backend.domain.task.dto.DbUpdateEvent;
import noonchissaum.backend.domain.task.service.AsyncTaskTxService;
import noonchissaum.backend.domain.task.service.DbEventListener;
import noonchissaum.backend.domain.wallet.service.WalletRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.math.BigDecimal;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class DbEventListenerUnitTest {

    @Mock
    private WalletRecordService walletRecordService;
    @Mock
    private BidRecordService bidRecordService;
    @Mock
    private BidService bidService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private AsyncTaskTxService asyncTaskTxService;

    @Test
    @DisplayName("DB 업데이트 이벤트 처리 시 bid/wallet 저장 후 성공 마킹 및 pending 제거")
    void handleWalletUpdate_processesAndCleansPendingKeys() {
        DbEventListener listener = new DbEventListener(
                walletRecordService, bidRecordService, bidService, redisTemplate, asyncTaskTxService);
        DbUpdateEvent event = new DbUpdateEvent(
                10L, 20L, BigDecimal.valueOf(15000), BigDecimal.valueOf(10000), 300L, "req-300");

        when(bidService.isExistRequestId("req-300")).thenReturn(false);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        listener.handleWalletUpdate(event);

        verify(asyncTaskTxService).startTask(event);
        verify(bidRecordService).saveBidRecord(300L, 10L, BigDecimal.valueOf(15000), "req-300");
        verify(walletRecordService).saveWalletRecord(10L, BigDecimal.valueOf(15000), 20L, BigDecimal.valueOf(10000), 300L);
        verify(asyncTaskTxService).markSuccess("req-300");
        verify(setOps).remove("pending:user:10", "req-300");
        verify(setOps).remove("pending:user:20", "req-300");
    }

    @Test
    @DisplayName("DB 업데이트 이벤트 처리 시 중복 requestId면 bid 저장 생략")
    void handleWalletUpdate_whenRequestAlreadyExists_skipsBidSave() {
        DbEventListener listener = new DbEventListener(
                walletRecordService, bidRecordService, bidService, redisTemplate, asyncTaskTxService);
        DbUpdateEvent event = new DbUpdateEvent(
                11L, -1L, BigDecimal.valueOf(12000), BigDecimal.ZERO, 301L, "req-301");

        when(bidService.isExistRequestId("req-301")).thenReturn(true);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOps);

        listener.handleWalletUpdate(event);

        verify(bidRecordService, never()).saveBidRecord(301L, 11L, BigDecimal.valueOf(12000), "req-301");
        verify(walletRecordService).saveWalletRecord(11L, BigDecimal.valueOf(12000), -1L, BigDecimal.ZERO, 301L);
        verify(setOps, never()).remove("pending:user:-1", "req-301");
    }
}
