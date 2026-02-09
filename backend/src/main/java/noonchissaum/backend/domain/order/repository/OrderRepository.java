package noonchissaum.backend.domain.order.repository;

import java.util.Optional;
import noonchissaum.backend.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDate;

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
    @Modifying
    @Query(value = """
        UPDATE orders o
        JOIN shipments s ON s.order_id = o.order_id
        SET o.status = 'COMPLETED',
            o.confirmed_at = :now
        WHERE o.status <> 'COMPLETED'
          AND s.status = 'DELIVERED'
          AND s.delivered_at <= :threshold
          AND o.confirmed_at IS NULL
    """, nativeQuery = true)
    int autoConfirmAfterDelivered(@Param("threshold") LocalDateTime threshold,
                                  @Param("now") LocalDateTime now);


}
