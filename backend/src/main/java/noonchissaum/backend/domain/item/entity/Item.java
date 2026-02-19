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

    @Column(name = "wish_count")
    private Integer wishCount;

    @Column(name = "status")
    private Boolean status;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    private List<ItemImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Wish> wishes = new ArrayList<>();

    @OneToOne(mappedBy = "item", fetch = FetchType.LAZY)
    private Auction auction;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(columnDefinition = "POINT SRID 4326", name = "item_location")
    private String itemLocation;

    @Column(length = 50)
    private String sellerDong;

    @Builder
    public Item(User seller, Category category, String title, String description) {
        this.seller = seller;
        this.category = category;
        this.title = title;
        this.description = description;
        this.status = true;
        this.wishCount = 0;
    }

    public void addImage(ItemImage image) {
        this.images.add(image);
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setSellerLocation(String location, String dong) {
        this.itemLocation = location;
        this.sellerDong = dong;
    }

    public void updateSellerLocation(String newLocation, String newDong) {
        this.itemLocation = newLocation;
        this.sellerDong = newDong;
    }

    public void delete() {
        if (Boolean.FALSE.equals(this.status)) {
            throw new IllegalStateException("Already deleted item.");
        }
        this.status = false;
    }

    public void restore() {
        if (Boolean.TRUE.equals(this.status)) {
            throw new IllegalStateException("Item is already active.");
        }
        this.status = true;
    }

    public void setSellerDong(String dong) {
        this.sellerDong = dong;
    }

    public boolean isDeleted() {
        return Boolean.FALSE.equals(this.status);
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(this.status);
    }

    public void changeCategory(Category category) {
        this.category = category;
    }
}
