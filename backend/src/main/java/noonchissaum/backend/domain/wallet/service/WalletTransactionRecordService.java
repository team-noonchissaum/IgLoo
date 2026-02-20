package noonchissaum.backend.domain.wallet.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.wallet.entity.*;
import noonchissaum.backend.domain.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletTransactionRecordService {

    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional
    public void record(
            Wallet wallet,
            TransactionType type,
            BigDecimal amount,
            Long refId
    ) {
        BigDecimal delta = type.apply(amount);
        WalletTransaction tx = WalletTransaction.create(
                wallet,
                delta,
                type,
                refId
        );
        walletTransactionRepository.save(tx);
    };
}
