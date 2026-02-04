package noonchissaum.backend.domain.order.dto.payment.req;

public record TossWebhookReq (
        String eventType,
        WebhookData data
) {

    public record WebhookData (
            String paymentKey,
            String orderId,
            Long amount,
            String status,
            String method
    ){}
}
