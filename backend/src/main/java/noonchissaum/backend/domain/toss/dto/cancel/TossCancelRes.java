package noonchissaum.backend.domain.toss.dto.cancel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossCancelRes(
        String paymentKey,
        String orderId,
        String status,
        Integer totalAmount
) {
}
