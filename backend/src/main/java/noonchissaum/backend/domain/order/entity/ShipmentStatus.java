package noonchissaum.backend.domain.order.entity;

public enum ShipmentStatus {
    READY, // 배송지 입력 완료/대기
    SHIPPED, // 발송(송장 입력) 완료
    DELIVERED // 배송완료(스마트택배 조회로 자동 반영?)
}
