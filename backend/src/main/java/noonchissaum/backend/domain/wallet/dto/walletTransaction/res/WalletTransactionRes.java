package noonchissaum.backend.domain.wallet.dto.walletTransaction.res;

import noonchissaum.backend.domain.wallet.entity.TransactionRefType;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.WalletTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletTransactionRes(
        Long transaction_id,
        BigDecimal amount,
        TransactionType type,
        TransactionRefType ref_type,
        Long ref_id,
        String memo,
        LocalDateTime created_at

) {
    public static WalletTransactionRes from (WalletTransaction walletTransaction){
        return new WalletTransactionRes(
                walletTransaction.getId(),
                walletTransaction.getAmount(),
                walletTransaction.getType(),
                walletTransaction.getRefType(),
                walletTransaction.getRefId(),
                walletTransaction.getMemo(),
                walletTransaction.getCreatedAt()
        );
    }
}
