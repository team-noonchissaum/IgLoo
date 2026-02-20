package noonchissaum.backend.domain.order.entity;

import jakarta.persistence.*;
import lombok.*;
import noonchissaum.backend.global.entity.BaseTimeEntity;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "shipments",
        indexes = {
                @Index(name = "idx_ship_order", columnList = "order_id"),
                @Index(name = "idx_ship_status", columnList = "status"),
                @Index(name = "idx_ship_delivered_at", columnList = "delivered_at"),
                @Index(name = "idx_ship_tracking", columnList = "carrier_code, tracking_number")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Shipment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipment_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    // 주소/수취인(카카오 우편번호 서비스 결과 저장)
    @Column(name = "recipient_name", length = 50)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "address1", length = 255)
    private String address1;

    @Column(name = "address2", length = 255)
    private String address2;

    @Column(name = "delivery_memo", length = 255)
    private String deliveryMemo;

    // ===== 송장/택배사 =====
    /**
     * 스마트택배(SweetTracker) 연동을 위해 "택배사 코드"를 저장하는 것을 권장
     * 예: "04"(CJ대한통운) 같은 t_code, 또는 서비스에서 정의한 코드 체계
     */
    @Column(name = "carrier_code", length = 30)
    private String carrierCode;

    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status = ShipmentStatus.READY;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;


    // 도메인 로직
    public void saveAddress(String name, String phone, String zip, String addr1, String addr2, String memo) {
        if (this.status != ShipmentStatus.READY) {
            throw new IllegalStateException("발송 이후에는 주소를 변경할 수 없습니다.");
        }
        this.recipientName = name;
        this.recipientPhone = phone;
        this.zipCode = zip;
        this.address1 = addr1;
        this.address2 = addr2;
        this.deliveryMemo = memo;
    }
    public boolean hasAddress() {
        return recipientName != null && !recipientName.isBlank()
                && recipientPhone != null && !recipientPhone.isBlank()
                && zipCode != null && !zipCode.isBlank()
                && address1 != null && !address1.isBlank();
    }
    // 판매자: 송장 입력 = 발송 시작
    public void inputInvoice(String carrierCode, String trackingNumber, LocalDateTime now) {
        if (!hasAddress()) {
            throw new IllegalStateException("배송지 정보가 없습니다.");
        }
        if (this.status != ShipmentStatus.READY) {
            throw new IllegalStateException("이미 송장이 입력되었습니다.");
        }
        this.carrierCode = carrierCode;
        this.trackingNumber = trackingNumber;
        this.status = ShipmentStatus.SHIPPED;
        this.shippedAt = now;
    }

    // 스케줄러: 스마트택배 조회 결과 배송완료로 판정
    public void markDelivered(LocalDateTime now) {
        if (this.status != ShipmentStatus.SHIPPED) return;
        this.status = ShipmentStatus.DELIVERED;
        this.deliveredAt = now;
    }
    public boolean canSyncTracking() {
        return this.status == ShipmentStatus.SHIPPED
                && carrierCode != null && !carrierCode.isBlank()
                && trackingNumber != null && !trackingNumber.isBlank();
    }

    public boolean hasRequestInfo() {
        return recipientName != null && !recipientName.isBlank()
                && recipientPhone != null && !recipientPhone.isBlank()
                && zipCode != null && !zipCode.isBlank()
                && address1 != null && !address1.isBlank();
    }


}
