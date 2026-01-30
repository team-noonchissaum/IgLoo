package noonchissaum.backend.domain.order.dto.payment.req;

public record PaymentAbortReq(
        Long paymentId,
        String reason
) {
}
