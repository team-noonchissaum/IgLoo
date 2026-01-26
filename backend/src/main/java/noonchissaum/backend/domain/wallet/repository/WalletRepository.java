package noonchissaum.backend.domain.wallet.repository;

import noonchissaum.backend.domain.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, String> {

    Optional<Wallet> findByUserId(Long userId);
}
