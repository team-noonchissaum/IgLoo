package noonchissaum.backend.domain.wallet.dto.res;

import noonchissaum.backend.domain.wallet.entity.Withdrawal;
import noonchissaum.backend.domain.wallet.entity.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WithdrawalRes(
        Long withdrawalId,
        BigDecimal amount,
        BigDecimal feeAmount,
        WithdrawalStatus status,
        LocalDateTime createdAt
) {
    public static WithdrawalRes from(Withdrawal withdrawal) {
        return new WithdrawalRes(
                withdrawal.getId(),
                withdrawal.getAmount(),
                withdrawal.getFeeAmount(),
                withdrawal.getStatus(),
                withdrawal.getCreatedAt()
        );
    }
}
