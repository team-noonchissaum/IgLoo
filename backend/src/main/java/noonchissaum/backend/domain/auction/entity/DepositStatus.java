package noonchissaum.backend.domain.auction.entity;

public enum DepositStatus {
    HELD,           // 등록 시 보증금 차감 완료(예치 상태)
    REFUNDED,       // 5분 이내 취소로 환불 완료
    FORFEITED       // 5분 이후 취소로 몰수 완료
}
