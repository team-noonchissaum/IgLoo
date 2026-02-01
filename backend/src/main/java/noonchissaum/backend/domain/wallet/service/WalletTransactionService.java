package noonchissaum.backend.domain.wallet.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.wallet.dto.walletTransaction.res.WalletTransactionRes;
import noonchissaum.backend.domain.wallet.dto.withdrawal.res.WithdrawalRes;
import noonchissaum.backend.domain.wallet.repository.WalletTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepository;

    public Page<WalletTransactionRes> getMyWalletTransaction(Long userId, Pageable pageable) {
        return walletTransactionRepository.findByWallet_User_Id(userId, pageable)
                .map(WalletTransactionRes::from);
    }
}
