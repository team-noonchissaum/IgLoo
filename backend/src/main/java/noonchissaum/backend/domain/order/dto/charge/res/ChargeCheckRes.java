package noonchissaum.backend.domain.order.dto.charge.res;

import noonchissaum.backend.domain.order.entity.ChargeCheck;
import noonchissaum.backend.domain.order.entity.CheckStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ChargeCheckRes(
        Long chargeCheckId,
        Long paymentId,
        BigDecimal amount,
        CheckStatus status,
        LocalDateTime createdAt,
        LocalDateTime expireAt
) {
    public static ChargeCheckRes from(ChargeCheck cc) {
        return new ChargeCheckRes(
                cc.getId(),
                cc.getPayment().getId(),
                cc.getPayment().getAmount(),
                cc.getStatus(),
                cc.getCreatedAt(),
                cc.getCreatedAt().plusDays(7)
        );
    }
}