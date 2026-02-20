package noonchissaum.backend.domain.settlement.entity;

import jakarta.persistence.*;
import lombok.*;
import noonchissaum.backend.domain.order.entity.Order;
import noonchissaum.backend.global.entity.BaseTimeEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "settlements",
        uniqueConstraints = @UniqueConstraint(name = "uk_settlement_order", columnNames = "order_id"),
        indexes = {
                @Index(name = "idx_settlement_status", columnList = "status"),
                @Index(name = "idx_settlement_seller", columnList = "seller_id"),
                @Index(name = "idx_settlement_created", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Settlement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "gross_amount", precision = 15, scale = 0, nullable = false)
    private BigDecimal grossAmount; // 낙찰가

    @Column(name = "fee_amount", precision = 15, scale = 0, nullable = false)
    private BigDecimal feeAmount; // 수수료(10%)

    @Column(name = "net_amount", precision = 15, scale = 0, nullable = false)
    private BigDecimal netAmount; // 판매자 정산액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

}
