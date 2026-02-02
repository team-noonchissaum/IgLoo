package noonchissaum.backend.domain.wallet.dto.wallet.res;

import noonchissaum.backend.domain.wallet.entity.Wallet;

import java.math.BigDecimal;

public record WalletRes(
        Long id,
        Long userId,
        BigDecimal balance,
        BigDecimal lockedBalance
) {
    public static WalletRes from (Wallet wallet){
        return new WalletRes(
                wallet.getId(),
                wallet.getUser().getId(),
                wallet.getBalance(),
                wallet.getLockedBalance()
        );
    }
}
