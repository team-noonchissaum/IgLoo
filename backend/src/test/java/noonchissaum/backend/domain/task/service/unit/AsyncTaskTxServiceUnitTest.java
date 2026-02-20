package noonchissaum.backend.domain.task.service.unit;

import noonchissaum.backend.domain.task.dto.DbUpdateEvent;
import noonchissaum.backend.domain.task.entity.AsyncTask;
import noonchissaum.backend.domain.task.repository.AsyncTaskRepository;
import noonchissaum.backend.domain.task.service.AsyncTaskTxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AsyncTaskTxServiceUnitTest {

    @Mock
    private AsyncTaskRepository asyncTaskRepository;

    @Test
    @DisplayName("작업 시작 처리 시 requestId 기존 작업이 있으면 기존 작업 반환")
    void startTask_whenTaskExists_returnsExistingTask() {
        AsyncTaskTxService service = new AsyncTaskTxService(asyncTaskRepository);
        DbUpdateEvent event = sampleEvent("req-exists");
        AsyncTask existing = new AsyncTask(event);
        when(asyncTaskRepository.findByRequestId("req-exists")).thenReturn(Optional.of(existing));

        AsyncTask result = service.startTask(event);

        assertThat(result).isEqualTo(existing);
    }

    @Test
    @DisplayName("작업 시작 처리 시 requestId 신규면 새 작업 저장")
    void startTask_whenTaskMissing_savesNewTask() {
        AsyncTaskTxService service = new AsyncTaskTxService(asyncTaskRepository);
        DbUpdateEvent event = sampleEvent("req-new");
        when(asyncTaskRepository.findByRequestId("req-new")).thenReturn(Optional.empty());
        when(asyncTaskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AsyncTask result = service.startTask(event);

        verify(asyncTaskRepository).save(any(AsyncTask.class));
        assertThat(ReflectionTestUtils.getField(result, "requestId")).isEqualTo("req-new");
    }

    @Test
    @DisplayName("작업 성공 마킹 시 작업이 없으면 IllegalStateException 예외 던짐")
    void markSuccess_whenTaskMissing_throwsIllegalStateException() {
        AsyncTaskTxService service = new AsyncTaskTxService(asyncTaskRepository);
        when(asyncTaskRepository.findByRequestId("req-missing")).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.markSuccess("req-missing"));

        assertThat(ex.getMessage()).contains("AsyncTask not found");
    }

    @Test
    @DisplayName("작업 성공 마킹 시 isSuccess를 true로 변경")
    void markSuccess_whenTaskExists_marksTaskAsSuccess() {
        AsyncTaskTxService service = new AsyncTaskTxService(asyncTaskRepository);
        AsyncTask task = new AsyncTask(sampleEvent("req-success"));
        when(asyncTaskRepository.findByRequestId("req-success")).thenReturn(Optional.of(task));

        service.markSuccess("req-success");

        assertThat(ReflectionTestUtils.getField(task, "isSuccess")).isEqualTo(true);
    }

    private DbUpdateEvent sampleEvent(String requestId) {
        return new DbUpdateEvent(1L, -1L, BigDecimal.valueOf(10000), BigDecimal.ZERO, 101L, requestId);
    }
}
