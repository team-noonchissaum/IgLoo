package noonchissaum.backend.domain.task.repository;

import noonchissaum.backend.domain.task.entity.AsyncTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AsyncTaskRepository extends JpaRepository<AsyncTask,Integer> {

    boolean existsByUserIdAndIsSuccess(long userId, boolean isSuccess);

}
