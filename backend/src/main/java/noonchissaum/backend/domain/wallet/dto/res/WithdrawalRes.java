package noonchissaum.backend.domain.wallet.dto.res;

import noonchissaum.backend.domain.wallet.entity.Withdrawal;
import noonchissaum.backend.domain.wallet.entity.WithdrawalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WithdrawalRes(
        Long withdrawalId,
        BigDecimal amount,
        BigDecimal feeAmount,
        String bankName,
        WithdrawalStatus status,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {
    public static WithdrawalRes from(Withdrawal withdrawal) {
        return new WithdrawalRes(
                withdrawal.getId(),
                withdrawal.getAmount(),
                withdrawal.getFeeAmount(),
                withdrawal.getBankName(),
                withdrawal.getStatus(),
                withdrawal.getCreatedAt(),
                withdrawal.getProcessedAt()
        );
    }
}
