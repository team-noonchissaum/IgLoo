package noonchissaum.backend.domain.order.entity;

import jakarta.persistence.*;
import lombok.*;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.entity.BaseTimeEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    //거래방식(직거래/배송)
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type")
    private DeliveryType deliveryType;

    // 양방향 매핑: 배송 정보 (1:1)
    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Shipment shipment;

    // 구매확정(수동/자동) 시각
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "final_price", precision = 15, scale = 0, nullable = false)
    private BigDecimal finalPrice;

    public void chooseDeliveryType(DeliveryType type){
        if(this.deliveryType != null){
            throw new IllegalStateException("이미 거래방식이 선택되었습니다.");
        }
        this.deliveryType = type;
    }
    // 배송완료 후 구매확정
    public void confirmAfterDelivered() {
        if (this.status == OrderStatus.COMPLETED) return; // 멱등
        this.status = OrderStatus.COMPLETED;
        this.confirmedAt = LocalDateTime.now();
    }

    // 직거래 후 구매확정

    public void confirmDirectTrade() {
        if (this.deliveryType != DeliveryType.DIRECT) {
            throw new IllegalStateException("직거래 주문만 구매확정이 가능합니다.");
        }
        if (this.status != OrderStatus.CREATED && this.status != OrderStatus.SHIPPED) {
            throw new IllegalStateException("현재 상태에서는 구매확정이 불가능합니다.");
        }
        this.status = OrderStatus.COMPLETED;
    }


}