package noonchissaum.backend.domain.order.repository;

import noonchissaum.backend.domain.order.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByOrder_Id(Long orderId);
    boolean existsByOrder_Id(Long orderId);

    // 배송상태 동기화 대상: SHIPPED + 송장 존재 + deliveredAt null
    @Query("""
    select s
    from Shipment s
    where s.status = noonchissaum.backend.domain.order.entity.ShipmentStatus.SHIPPED
      and s.deliveredAt is null
      and s.carrierCode is not null and s.carrierCode <> ''
      and s.trackingNumber is not null and s.trackingNumber <> ''
    order by s.id asc
""")
    List<Shipment> findDeliveredSyncTargets(Pageable pageable);
}
