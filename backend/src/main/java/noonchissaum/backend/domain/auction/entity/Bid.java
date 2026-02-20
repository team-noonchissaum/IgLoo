package noonchissaum.backend.domain.auction.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.entity.BaseTimeEntity;
import java.math.BigDecimal;

@Entity
@Table(name = "bids")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bid extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id")
    private User bidder;

    @Column(name = "bid_price", precision = 15, scale = 0)
    private BigDecimal bidPrice;

    @Column(nullable = false)
    private String requestId;

    public Bid(Auction auction, User bidder, BigDecimal bidPrice , String requestId) {
        this.auction = auction;
        this.bidder = bidder;
        this.bidPrice = bidPrice;
        this.requestId  = requestId;

    }
}
