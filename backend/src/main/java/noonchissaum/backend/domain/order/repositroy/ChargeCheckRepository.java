package noonchissaum.backend.domain.order.repositroy;

import noonchissaum.backend.domain.order.entity.ChargeCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChargeCheckRepository extends JpaRepository<ChargeCheck, Long> {
    boolean existsByPaymentId(Long paymentId);
}
