package noonchissaum.backend.domain.user.repository;


import noonchissaum.backend.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
        SELECT COUNT(o)
        FROM Order o
        WHERE DATE(o.createdAt) = :date
    """)
    long countByDate(@Param("date") LocalDate date);
}