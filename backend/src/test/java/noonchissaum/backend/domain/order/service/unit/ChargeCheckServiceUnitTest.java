package noonchissaum.backend.domain.order.service.unit;

import noonchissaum.backend.domain.order.service.ChargeCheckService;
import noonchissaum.backend.domain.order.service.ChargeRecordService;
import noonchissaum.backend.domain.task.service.TaskService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.util.UserLockExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ChargeCheckServiceUnitTest {

    @Mock
    private TaskService taskService;
    @Mock
    private ChargeRecordService chargeRecordService;
    @Mock
    private noonchissaum.backend.domain.order.repository.ChargeCheckRepository chargeCheckRepository;
    @Mock
    private UserLockExecutor userLockExecutor;

    @Test
    @DisplayName("충전 확인 처리 시 pending 작업이 있으면 PENDING_TASK_EXISTS 예외 던짐")
    void confirmCharge_whenPendingTaskExists_throwsApiException() {
        ChargeCheckService service = new ChargeCheckService(taskService, chargeRecordService, chargeCheckRepository, userLockExecutor);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(userLockExecutor).withUserLock(eq(1L), any(Runnable.class));
        when(taskService.checkTasks(1L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> service.confirmCharge(10L, 1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PENDING_TASK_EXISTS);
        verify(chargeRecordService, never()).confirmChargeTx(10L, 1L);
    }

    @Test
    @DisplayName("충전 취소 처리 시 pending 작업이 없으면 취소 트랜잭션 호출")
    void cancelCharge_whenTaskClean_callsCancelTx() {
        ChargeCheckService service = new ChargeCheckService(taskService, chargeRecordService, chargeCheckRepository, userLockExecutor);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(userLockExecutor).withUserLock(eq(2L), any(Runnable.class));
        when(taskService.checkTasks(2L)).thenReturn(true);

        service.cancelCharge(22L, 2L, "단순 취소");

        verify(chargeRecordService).cancelChargeTx(22L, 2L, "단순 취소");
    }
}
