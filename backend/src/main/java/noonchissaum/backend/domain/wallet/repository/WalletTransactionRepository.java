package noonchissaum.backend.domain.wallet.repository;

import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Optional<WalletTransaction> findByTypeAndRefId(TransactionType type, Long refId);

    Page<WalletTransaction> findByWallet_User_Id(Long id, Pageable pageable);
}
