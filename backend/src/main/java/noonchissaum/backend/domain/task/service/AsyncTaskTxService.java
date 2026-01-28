package noonchissaum.backend.domain.task.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.task.dto.DbUpdateEvent;
import noonchissaum.backend.domain.task.entity.AsyncTask;
import noonchissaum.backend.domain.task.repository.AsyncTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AsyncTaskTxService {

    private final AsyncTaskRepository asyncTaskRepository;

    /**
     * requestId 기준으로 Task를 "무조건" DB에 남긴다.
     * - 이 메서드는 REQUIRES_NEW라서,
     *   본 작업 트랜잭션이 나중에 롤백돼도 task row는 남는다.
     *
     * 전제: tasks 테이블에 request_id 유니크 권장(중복 생성 방지)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AsyncTask startTask(DbUpdateEvent event) {
        // 중복 재시도 대비(권장): requestId로 이미 있으면 재사용
        return asyncTaskRepository.findByRequestId(event.requestId())
                .orElseGet(() -> asyncTaskRepository.save(new AsyncTask(event)));
    }

    /**
     * 성공 마킹도 독립 트랜잭션으로 확정
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(String requestId) {
        AsyncTask task = asyncTaskRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("AsyncTask not found. requestId=" + requestId));
        task.taskSuccess();
        // dirty checking으로 업데이트됨
    }
}