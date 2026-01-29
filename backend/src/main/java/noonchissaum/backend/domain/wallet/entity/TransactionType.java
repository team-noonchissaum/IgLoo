package noonchissaum.backend.domain.wallet.entity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionType {

    CHARGE("충전", TransactionRefType.CHARGE),

    BID_HOLD("입찰 홀딩", TransactionRefType.AUCTION),
    BID_RELEASE("입찰 해제/환급", TransactionRefType.AUCTION),

    DEPOSIT_LOCK("보증금 락", TransactionRefType.AUCTION),
    DEPOSIT_RETURN("보증금 반환", TransactionRefType.AUCTION),
    DEPOSIT_FORFEIT("보증금 몰수", TransactionRefType.AUCTION),

    WITHDRAW("출금", TransactionRefType.WITHDRAW),

    SETTLEMENT_IN("정산 입금", TransactionRefType.ORDER),
    SETTLEMENT_OUT("정산 출금", TransactionRefType.ORDER);

    private final String memo;
    private final TransactionRefType defaultRefType;
}
