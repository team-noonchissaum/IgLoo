package noonchissaum.backend.domain.wallet.repository;

import noonchissaum.backend.domain.wallet.entity.Withdrawal;
import noonchissaum.backend.domain.wallet.entity.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    @EntityGraph(attributePaths = {"wallet", "wallet.user"})
    Page<Withdrawal> findByWallet_User_Id(Long userId, Pageable pageable);

    Page<Withdrawal> findByStatus(WithdrawalStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"wallet", "wallet.user"})
    Optional<Withdrawal> findWithLockById(Long id);
}
