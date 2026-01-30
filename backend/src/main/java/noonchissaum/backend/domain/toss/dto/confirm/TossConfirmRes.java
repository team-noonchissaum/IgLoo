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
        String method,
        Receipt receipt,
        VirtualAccount virtualAccount,
        Card card// 카드 정보 (없으면 null)

) {
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Receipt {
        private String url;
    }

    public record VirtualAccount(
            String accountNumber,
            String bank,
            String customerName,
            String dueDate
    ) {}

    public record Card(
            String company,
            String number,
            Integer installmentPlanMonths,
            String approveNo
    ) {}
}
