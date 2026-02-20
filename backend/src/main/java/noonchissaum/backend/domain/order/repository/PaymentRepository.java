package noonchissaum.backend.domain.order.repository;

import jakarta.persistence.LockModeType;
import noonchissaum.backend.domain.order.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPgOrderId(String pgOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.pgOrderId = :pgOrderId")
    Optional<Payment> findByPgOrderIdWithLock(@Param("pgOrderId") String pgOrderId);
}
