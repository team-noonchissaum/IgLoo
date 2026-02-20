package noonchissaum.backend.domain.order.repository;

import java.util.Optional;
import noonchissaum.backend.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {

    @Query("""
        SELECT COUNT(o)
        FROM Order o
        WHERE DATE(o.createdAt) = :date
    """)
    long countByDate(@Param("date") LocalDate date);

    Optional<Order> findByIdAndBuyerId(Long orderId, Long buyerId);

    Optional<Order> findByIdAndSellerId(Long orderId, Long sellerId);
    // 배송완료 + 3일 자동확정 (deliveredAt 기준)
    // 대상 조회 추가
    @Query(value = """
        SELECT o.order_id
        FROM orders o
        JOIN shipments s ON s.order_id = o.order_id
        WHERE o.confirmed_at IS NULL
            AND o.status <> 'CANCELED'
            AND s.status = 'DELIVERED'
            AND s.delivered_at <= :threshold
        ORDER BY o.order_id ASC
        LIMIT 200
    """, nativeQuery = true)
    List<Long> findAutoConfirmTargets(@Param("threshold") LocalDateTime threshold);

    Optional<Order> findByAuction_IdAndBuyer_Id(Long auctionId, Long buyerId);

    Optional<Order> findByAuction_IdAndSeller_Id(Long auctionId, Long sellerId);
}
