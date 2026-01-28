package noonchissaum.backend.domain.wallet.repository;

import noonchissaum.backend.domain.wallet.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    @Query("""
        SELECT COALESCE(SUM(w.amount), 0)
        FROM WalletTransaction w
        WHERE w.type = 'USE'
        AND DATE(w.createdAt) = :date
    """)
    BigDecimal sumUsageByDate(@Param("date") LocalDate date);
}
