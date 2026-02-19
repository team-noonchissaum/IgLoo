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
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuctionRes {
    private Long auctionId;
    private String title;
    private String description;
    private BigDecimal currentPrice;
    private BigDecimal startPrice;
    private Integer bidCount;
    private AuctionStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long sellerId;
    private String sellerNickname;
    private String sellerDong;
    private List<String> imageUrls;
    private Long itemId;
    private Long categoryId;
    private String categoryName;
    private Integer wishCount;
    private Boolean isWished;
    private List<AuctionRes> recommendedAuctions;

    public static AuctionRes from(Auction auction) {
        Item item = auction.getItem();
        return AuctionRes.builder()
                .auctionId(auction.getId())
                .itemId(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .currentPrice(auction.getCurrentPrice())
                .startPrice(auction.getStartPrice())
                .bidCount(auction.getBidCount())
                .status(auction.getStatus())
                .startAt(auction.getStartAt())
                .endAt(auction.getEndAt())
                .sellerId(item.getSeller().getId())
                .sellerNickname(item.getSeller().getNickname())
                .sellerDong(item.getSeller().getDong())
                .imageUrls(item.getImages().stream()
                        .map(ItemImage::getImageUrl)
                        .collect(Collectors.toList()))
                .categoryId(item.getCategory().getId())
                .categoryName(item.getCategory().getName())
                .wishCount(item.getWishCount())
                .isWished(null)
                .build();
    }

    public static AuctionRes from(Auction auction, boolean isWished) {
        Item item = auction.getItem();
        return AuctionRes.builder()
                .auctionId(auction.getId())
                .itemId(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .currentPrice(auction.getCurrentPrice())
                .startPrice(auction.getStartPrice())
                .bidCount(auction.getBidCount())
                .status(auction.getStatus())
                .startAt(auction.getStartAt())
                .endAt(auction.getEndAt())
                .sellerId(item.getSeller().getId())
                .sellerNickname(item.getSeller().getNickname())
                .sellerDong(item.getSeller().getDong())
                .imageUrls(item.getImages().stream()
                        .map(ItemImage::getImageUrl)
                        .collect(Collectors.toList()))
                .categoryId(item.getCategory().getId())
                .categoryName(item.getCategory().getName())
                .wishCount(item.getWishCount() == null ? 0 : item.getWishCount())
                .isWished(isWished)
                .build();
    }

    public static AuctionRes from(Auction auction, boolean isWished, List<AuctionRes> recommendedAuctions) {
        Item item = auction.getItem();
        return AuctionRes.builder()
                .auctionId(auction.getId())
                .itemId(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .currentPrice(auction.getCurrentPrice())
                .startPrice(auction.getStartPrice())
                .bidCount(auction.getBidCount())
                .status(auction.getStatus())
                .startAt(auction.getStartAt())
                .endAt(auction.getEndAt())
                .sellerId(item.getSeller().getId())
                .sellerNickname(item.getSeller().getNickname())
                .imageUrls(item.getImages().stream()
                        .map(ItemImage::getImageUrl)
                        .collect(Collectors.toList()))
                .categoryId(item.getCategory().getId())
                .categoryName(item.getCategory().getName())
                .wishCount(item.getWishCount() == null ? 0 : item.getWishCount())
                .isWished(isWished)
                .recommendedAuctions(recommendedAuctions)
                .build();
    }
}
