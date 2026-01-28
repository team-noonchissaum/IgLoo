package noonchissaum.backend.domain.auction.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.entity.BaseTimeEntity;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "auctions")
@Getter
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

    @Column(name = "imminent_minutes", nullable = false)
    private Integer imminentMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status;

    // 보증금 상태 필드
    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_status", nullable = false, length = 20)
    private DepositStatus depositStatus;

    // 양방향 매핑: 입찰 내역
    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL)
    private List<Bid> bids = new ArrayList<>();

    @Builder
    public Auction(Item item, BigDecimal startPrice, LocalDateTime startAt, LocalDateTime endAt) {
        this.item = item;
        this.currentPrice = startPrice;
        this.bidCount = 0;
        this.startAt = startAt;
        this.endAt = endAt;
        this.isExtended = false;
        this.imminentMinutes = ThreadLocalRandom.current().nextInt(5,8);
        this.status = AuctionStatus.READY;
        this.depositStatus = DepositStatus.HELD;

    }

    public void cancel() {
        this.status = AuctionStatus.CANCELED;
    }

    // 상태 전환 메서드
    public void run(){
        if(this.status != AuctionStatus.READY){
            return;
        }
        this.status = AuctionStatus.RUNNING;
        this.startAt = LocalDateTime.now();
    }
    /**
     * 상태 전이 메서드 (중복 처리 방지)
     */
    public void refundDeposit() {
        if (this.depositStatus != DepositStatus.HELD) {
            throw new IllegalStateException("Deposit already finalized: " + this.depositStatus);
        }
        this.depositStatus = DepositStatus.REFUNDED;
    }

    public void forfeitDeposit() {
        if (this.depositStatus != DepositStatus.HELD) {
            throw new IllegalStateException("Deposit already finalized: " + this.depositStatus);
        }
        this.depositStatus = DepositStatus.FORFEITED;
    }


    /**
     * 마감 임박 시 단 한 번만 3분 연장
     * 입찰 시점에서 호출
     */
    public void extendIfNeeded(LocalDateTime now) {
        if (Boolean.TRUE.equals(this.isExtended)) return;
        if (now.isAfter(this.endAt)) return;

        long remainSeconds = Duration.between(now, this.endAt).getSeconds();

        int windowMinutes = (this.imminentMinutes == null ? 5 : this.imminentMinutes);
        long windowSeconds = windowMinutes * 60L;

        if (remainSeconds <= windowSeconds) {
            this.endAt = this.endAt.plusMinutes(3);
            this.isExtended = true;
        }
    }

    /**
     * 새로운 bid가 생겼을 때 상태 정보 수정하기
     */
    public void updateBid(User user, BigDecimal newBid){
        this.currentBidder = user;
        this.currentPrice = newBid;
        this.bidCount++;
    }

    public User getSeller() {
        return this.item != null ? this.item.getSeller() : null;
    }
}
