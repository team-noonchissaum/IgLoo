package noonchissaum.backend.domain.auction.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.entity.ItemImage;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuctionListRes {

    private Long auctionId;
    private Long itemId;

    private String title;
    private BigDecimal currentPrice;
    private BigDecimal startPrice;
    private Integer bidCount;
    private AuctionStatus status;
    private LocalDateTime endAt;

    private String sellerNickname;
    private String thumbnailUrl;

    private Long categoryId;
    private String categoryName;

    private Integer wishCount;
    private Boolean isWished;


    public static AuctionListRes from(Auction auction) {
        Item item = auction.getItem();

        String thumbnailUrl = item.getImages().stream()
                .filter(img -> img.getSortOrder() != null && img.getSortOrder() == 0)
                .map(ItemImage::getImageUrl)
                .findFirst()
                .orElse(null);

        return AuctionListRes.builder()
                .auctionId(auction.getId())
                .itemId(item.getId())
                .title(item.getTitle())
                .currentPrice(auction.getCurrentPrice())
                .startPrice(item.getStartPrice())
                .bidCount(auction.getBidCount())
                .status(auction.getStatus())
                .endAt(auction.getEndAt())
                .sellerNickname(item.getSeller().getNickname())
                .thumbnailUrl(thumbnailUrl)
                .categoryId(item.getCategory().getId())
                .categoryName(item.getCategory().getName())
                .wishCount(item.getWishCount() == null ? 0 : item.getWishCount())
                .isWished(null)
                .build();
    }


    public static AuctionListRes from(Auction auction, boolean isWished) {
        AuctionListRes res = from(auction);
        res.isWished = isWished;
        return res;
    }
}
