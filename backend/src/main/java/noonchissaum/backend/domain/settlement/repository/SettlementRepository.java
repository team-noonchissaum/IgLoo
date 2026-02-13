package noonchissaum.backend.domain.settlement.repository;

import noonchissaum.backend.domain.settlement.entity.Settlement;
import noonchissaum.backend.domain.settlement.entity.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    boolean existsByOrder_Id(Long orderId);
    List<Settlement> findAllBySellerIdOrderByCreatedAtDesc(Long sellerId);
    List<Settlement> findAllByStatus(SettlementStatus status);
}
