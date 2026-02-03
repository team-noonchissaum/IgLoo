package noonchissaum.backend.domain.wallet.repository;

import noonchissaum.backend.domain.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    @Query("SELECT w.balance FROM Wallet w WHERE w.user.id = :userId")
    Optional<BigDecimal> findBalanceByUserId(@Param("userId") Long userId);

    @Query("""
        select (w.balance - w.lockedBalance)
        from Wallet w
        where w.user.id = :userId
    """)
    Optional<BigDecimal> findAvailableBalanceByUserId(@Param("userId") Long userId);
    boolean existsByUserId(Long userId);
}
