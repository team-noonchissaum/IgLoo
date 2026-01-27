package noonchissaum.backend.domain.auction.entity;

import jakarta.persistence.*;
import lombok.*;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.entity.BaseTimeEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "auctions")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auction_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "current_price", precision = 15, scale = 0)
    private BigDecimal currentPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_bidder_id")
    private User currentBidder;

    @Column(name = "bid_count")
    private Integer bidCount;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "is_extended")
    private Boolean isExtended;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status;

    // 양방향 매핑: 입찰 내역
    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL)
    private List<Bid> bids = new ArrayList<>();

    @Builder
    public Auction(AuctionStatus status, BigDecimal currentPrice, LocalDateTime endAt) {
        this.status = status;
        this.currentPrice = currentPrice;
        this.endAt = endAt;
    }
}