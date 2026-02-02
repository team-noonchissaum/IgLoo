package noonchissaum.backend.domain.wallet.entity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum TransactionType {

    CHARGE("충전", TransactionRefType.CHARGE, +1),

    BID_HOLD("입찰 홀딩", TransactionRefType.BID, -1),
    BID_RELEASE("입찰 취소", TransactionRefType.BID, +1),

    DEPOSIT_LOCK("보증금 락", TransactionRefType.AUCTION, -1),
    DEPOSIT_RETURN("보증금 반환", TransactionRefType.AUCTION, +1),
    DEPOSIT_FORFEIT("보증금 몰수", TransactionRefType.AUCTION, -1),

    WITHDRAW_REQUEST("출금 신청", TransactionRefType.WITHDRAW, -1),
    WITHDRAW_REJECT("출금 거부", TransactionRefType.WITHDRAW, +1),
    WITHDRAW_CONFIRM("출금 성공", TransactionRefType.WITHDRAW, -1),

    SETTLEMENT_IN("판매 정산금 지급", TransactionRefType.ORDER, +1),
    SETTLEMENT_OUT("제품 구매", TransactionRefType.ORDER, -1);

    private final String memo;
    private final TransactionRefType defaultRefType;
    private final int sign;

    public BigDecimal apply(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return amount.multiply(BigDecimal.valueOf(sign));
    }
}
