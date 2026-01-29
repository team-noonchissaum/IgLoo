package noonchissaum.backend.domain.item.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
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

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Builder
    public Item(User seller, Category category, String title, String description, BigDecimal startPrice) {
        this.seller = seller;
        this.category = category;
        this.title = title;
        this.description = description;
        this.startPrice = startPrice;
        this.status = true;
        this.wishCount = 0;
    }

    public void addImage(ItemImage image) {
        this.images.add(image);
        if (image.getItem() != this) {
            // ItemImage 엔티티에 setItem 메서드가 필요할 수 있음. 혹은 생성자 시점에 처리.
            // 여기서는 ItemImage 생성자에서 처리한다고 가정하고 add만 수행하거나,
            // ItemImage setter가 있다면 호출. 현재 ItemImage에는 setter가 없으므로 add만 수행.
        }
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    /**관리자 계정용*/
    public void delete() {
        if (Boolean.FALSE.equals(this.status)) {
            throw new IllegalStateException("이미 삭제된 상품입니다.");
        }
        this.status = false;
    }

    public void restore() {
        if (Boolean.TRUE.equals(this.status)) {
            throw new IllegalStateException("이미 활성 상태인 상품입니다.");
        }
        this.status = true;
    }

    public boolean isDeleted() {
        return Boolean.FALSE.equals(this.status);
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(this.status);
    }
}
