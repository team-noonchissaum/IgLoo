package noonchissaum.backend.domain.wallet.repository;

import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    @Query("""
        SELECT COALESCE(SUM(w.amount), 0)
        FROM WalletTransaction w
        WHERE w.type = 'USE'
        AND DATE(w.createdAt) = :date
    """)
    BigDecimal sumUsageByDate(@Param("date") LocalDate date);

    Optional<WalletTransaction> findByTypeAndRefId(TransactionType type, Long refId);

    Page<WalletTransaction> findByWallet_User_Id(Long id, Pageable pageable);
}
