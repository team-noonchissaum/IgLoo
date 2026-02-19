package noonchissaum.backend.domain.task.service.unit;

import noonchissaum.backend.domain.task.repository.AsyncTaskRepository;
import noonchissaum.backend.domain.task.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class TaskServiceUnitTest {

    @Mock
    private AsyncTaskRepository asyncTaskRepository;
    @Mock
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("대기 작업 확인 시 Redis/DB 모두 깨끗하면 true 반환")
    void checkTasks_whenRedisAndDbAreClean_returnsTrue() {
        TaskService service = new TaskService(asyncTaskRepository, redisTemplate);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.size("pending:user:1")).thenReturn(0L);
        when(asyncTaskRepository.existsByUserIdAndIsSuccess(1L, false)).thenReturn(false);

        boolean result = service.checkTasks(1L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("대기 작업 확인 시 Redis 또는 DB에 미완료 작업이 있으면 false 반환")
    void checkTasks_whenPendingExists_returnsFalse() {
        TaskService service = new TaskService(asyncTaskRepository, redisTemplate);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.size("pending:user:2")).thenReturn(1L);
        when(asyncTaskRepository.existsByUserIdAndIsSuccess(2L, false)).thenReturn(false);

        boolean result = service.checkTasks(2L);

        assertThat(result).isFalse();
    }
}
