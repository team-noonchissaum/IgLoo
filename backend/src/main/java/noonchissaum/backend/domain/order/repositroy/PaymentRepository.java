package noonchissaum.backend.domain.order.repositroy;

import noonchissaum.backend.domain.order.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPgOrderId(String pgOrderId);
}
