package noonchissaum.backend.domain.order.repositroy;

import java.util.Optional;
import noonchissaum.backend.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


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
}
