package noonchissaum.backend.domain.item.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.entity.ItemImage;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;

@Getter
@AllArgsConstructor
public class WishItemRes {

    private Long itemId;
    private String title;
    private BigDecimal startPrice;
    private String sellerName;
    private String thumbnailUrl;
    private Integer wishCount;
    private Boolean status;

    public static WishItemRes from(Item item) {
        return new WishItemRes(
                item.getId(),
                item.getTitle(),
                item.getStartPrice(),
                item.getSeller().getNickname(),
                extractThumbnail(item),
                item.getWishCount(),
                item.getStatus()
        );
    }

    /**
     * 대표 이미지 추출 로직
     * - sortOrder가 가장 작은 이미지
     * - 없으면 null
     */
    private static String extractThumbnail(Item item) {
        if (item.getImages() == null || item.getImages().isEmpty()) {
            return null;
        }

        Optional<ItemImage> thumbnail = item.getImages().stream()
                .min(Comparator.comparing(
                        img -> img.getSortOrder() == null ? Integer.MAX_VALUE : img.getSortOrder()
                ));

        return thumbnail.map(ItemImage::getImageUrl).orElse(null);
    }
}
