package noonchissaum.backend.domain.order.dto.payment.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import noonchissaum.backend.domain.order.entity.PgProvider;

public record PaymentPrepareReq (
        @NotNull
        @Min(1)
        int amount,
        @NotNull
        PgProvider provider
) {
}
