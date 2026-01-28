package noonchissaum.backend.domain.item.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.entity.BaseTimeEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(length = 255)
    private String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "start_price", precision = 15, scale = 0)
    private BigDecimal startPrice;

    @Column(name = "wish_count")
    private Integer wishCount;

    /**
     * true: ACTIVE
     * false: DELETED
     */
    @Column(name = "status")
    private Boolean status;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    private List<ItemImage> images = new ArrayList<>();

    // 양방향 매핑: 이 상품에 대한 찜 내역
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Wish> wishes = new ArrayList<>();

    // 양방향 매핑: 이 상품에 연결된 경매 (1:1)
    @OneToOne(mappedBy = "item", fetch = FetchType.LAZY)
    private Auction auction;

    public Item(User seller, Category category, String title, BigDecimal startPrice) {
        this.seller = seller;
        this.category = category;
        this.title = title;
        this.startPrice = startPrice;
        this.status = true;
    }
}