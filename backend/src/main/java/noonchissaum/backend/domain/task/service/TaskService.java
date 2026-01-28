package noonchissaum.backend.domain.task.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.task.repository.AsyncTaskRepository;
import noonchissaum.backend.global.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final AsyncTaskRepository taskRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional(readOnly = true)
    public boolean checkTasks(Long userId) {

        Long pendingCount = redisTemplate.opsForSet().size(RedisKeys.pendingUser(userId));
        boolean isRedisClean = (pendingCount == null || pendingCount == 0);

        // 2. DB에 아직 성공하지 못한(isSuccess = false) 작업이 하나도 없어야 함
        // 주의: existsBy...는 존재하면 true를 반환하므로, !를 붙여서 "없어야 함"을 체크해야 합니다.
        boolean isDbClean = !taskRepository.existsByUserIdAndIsSuccess(userId, false);

        // 둘 다 깨끗해야(true) 정산/충전 가능
        return isRedisClean && isDbClean;
    }
}
