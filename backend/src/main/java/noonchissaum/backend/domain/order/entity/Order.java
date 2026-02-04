package noonchissaum.backend.domain.order.entity;

import jakarta.persistence.*;
import lombok.*;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.entity.BaseTimeEntity;

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

    public void chooseDeliveryType(DeliveryType type){
        if(this.deliveryType != null){
            throw new IllegalStateException("이미 거래방식이 선택되었습니다.");
        }
        this.deliveryType = type;
    }
}