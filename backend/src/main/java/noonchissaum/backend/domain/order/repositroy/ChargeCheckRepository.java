package noonchissaum.backend.domain.order.repositroy;

import jakarta.persistence.LockModeType;
import noonchissaum.backend.domain.order.entity.ChargeCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChargeCheckRepository extends JpaRepository<ChargeCheck, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cc from ChargeCheck cc " +
            "join fetch cc.user " +
            "join fetch cc.payment " +
            "where cc.id = :id")
    Optional<ChargeCheck> findWithLockById(@Param("id") Long id);
}
