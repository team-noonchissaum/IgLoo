package noonchissaum.backend.domain.wallet.entity;

public enum TransactionType {
    CHARGE,
    BID_HOLD,
    BID_RELEASE,
    DEPOSIT_LOCK,
    DEPOSIT_RETURN,
    DEPOSIT_FORFEIT,
    WITHDRAW,
    SETTLEMENT_IN,
    SETTLEMENT_OUT
}
