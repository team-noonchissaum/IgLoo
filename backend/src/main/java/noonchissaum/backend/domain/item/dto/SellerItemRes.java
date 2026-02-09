package noonchissaum.backend.domain.item.dto;

import noonchissaum.backend.domain.item.entity.Item;

public record SellerItemRes(
        Long itemId,
        String itemName,
        String itemStatus,
        String imageUrl
) {
    public static SellerItemRes from(Item item) {
        return new SellerItemRes(
                item.getId(),
                item.getTitle(),
                item.getStatus() ? "ACTIVE" : "DELETED",
                item.getImages().isEmpty() ? null : item.getImages().get(0).getImageUrl()
        );
    }
}
