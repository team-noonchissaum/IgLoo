package noonchissaum.backend.domain.toss.dto.confirm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossConfirmRes(
        String paymentKey,
        String orderId,
        String status,
        Integer totalAmount,
        String approvedAt,
        Receipt receipt
) {
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Receipt {
        private String url;
    }
}
