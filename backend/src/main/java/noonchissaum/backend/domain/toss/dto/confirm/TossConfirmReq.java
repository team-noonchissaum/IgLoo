package noonchissaum.backend.domain.toss.dto.confirm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TossConfirmReq(
        String paymentKey,
        @JsonProperty("orderId")
        String pgOrderId,
        int amount
) {
}
