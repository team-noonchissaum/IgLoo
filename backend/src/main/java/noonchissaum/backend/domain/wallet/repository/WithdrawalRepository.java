package noonchissaum.backend.domain.wallet.repository;

import noonchissaum.backend.domain.wallet.entity.Withdrawal;
import noonchissaum.backend.domain.wallet.entity.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    Page<Withdrawal> findByWallet_User_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Withdrawal> findByStatusOrderByCreatedAtAsc(WithdrawalStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Withdrawal> findWithLockById(Long id);
}
