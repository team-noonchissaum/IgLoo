package noonchissaum.backend.domain.order.dto.payment.res;

import noonchissaum.backend.domain.toss.dto.confirm.TossConfirmRes;

public record VirtualAccountInfo(
        String bank,
        String accountNumber,
        String customerName,
        String dueDate
) {
    // Toss에서 받은 객체를 DTO로 변환하는 정적 팩토리 메서드
    public static VirtualAccountInfo from(TossConfirmRes.VirtualAccount tossAccount) {
        return new VirtualAccountInfo(
                tossAccount.bank(),
                tossAccount.accountNumber(),
                tossAccount.customerName(),
                tossAccount.dueDate()
        );
    }
}