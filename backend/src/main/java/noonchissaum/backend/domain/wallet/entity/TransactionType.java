package noonchissaum.backend.domain.wallet.entity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionType {

    CHARGE("충전", TransactionRefType.CHARGE),

    BID_HOLD("입찰 홀딩", TransactionRefType.BID),
    BID_RELEASE("입찰 해제/환급", TransactionRefType.BID),

    DEPOSIT_LOCK("보증금 락", TransactionRefType.AUCTION),
    DEPOSIT_RETURN("보증금 반환", TransactionRefType.AUCTION),
    DEPOSIT_FORFEIT("보증금 몰수", TransactionRefType.AUCTION),

    WITHDRAW_REQUEST("출금 신청", TransactionRefType.WITHDRAW),
    WITHDRAW_REJECT("출금 거부", TransactionRefType.WITHDRAW),
    WITHDRAW_CONFIRM("출금 성공", TransactionRefType.WITHDRAW),

    SETTLEMENT_IN("판매 정산금 지급", TransactionRefType.ORDER),
    SETTLEMENT_OUT("제품 구매", TransactionRefType.ORDER);

    private final String memo;
    private final TransactionRefType defaultRefType;
}
