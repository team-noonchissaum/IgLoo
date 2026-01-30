package noonchissaum.backend.domain.order.dto.payment.req;

import jakarta.validation.constraints.NotNull;

public record PaymentConfirmReq(
        @NotNull
        String pgOrderId,
        @NotNull
        String paymentKey,
        Integer amount
) {
}
