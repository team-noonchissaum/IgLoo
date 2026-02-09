package noonchissaum.backend.domain.order.repository;

import noonchissaum.backend.domain.order.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByOrder_Id(Long orderId);
    boolean existsByOrder_Id(Long orderId);
}
