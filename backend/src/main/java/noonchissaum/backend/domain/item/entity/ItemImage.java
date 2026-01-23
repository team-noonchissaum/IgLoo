package noonchissaum.backend.domain.item.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.global.entity.BaseTimeEntity;

@Entity
@Table(name = "item_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
