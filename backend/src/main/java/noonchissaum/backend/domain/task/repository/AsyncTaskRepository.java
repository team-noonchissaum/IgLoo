package noonchissaum.backend.domain.task.repository;

import noonchissaum.backend.domain.task.entity.AsyncTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsyncTaskRepository extends JpaRepository<AsyncTask,Integer> {
    Optional<AsyncTask>findByRequestId(String requestId);

    boolean existsByUserIdAndIsSuccess(long userId, boolean isSuccess);

}
