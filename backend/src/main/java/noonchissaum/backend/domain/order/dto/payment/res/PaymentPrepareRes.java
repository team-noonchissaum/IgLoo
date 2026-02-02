package noonchissaum.backend.domain.order.dto.payment.res;

public record PaymentPrepareRes(
        Long paymentId,
        String pgOrderId
) {
}
