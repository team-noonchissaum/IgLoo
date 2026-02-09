package noonchissaum.backend.domain.item.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.dto.req.AuctionRegisterReq;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.entity.ItemImage;
import noonchissaum.backend.domain.item.repository.ItemImageRepository;
import noonchissaum.backend.domain.item.repository.ItemRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;

    public Item getActiveById(Long itemId){
        return itemRepository.findByIdAndStatusTrue(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));
    }

    public Item getById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));
    }

    //상품 정보 생성 및 이미지 등록
    public Item createItem(User seller, Category category, AuctionRegisterReq request) {
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title(request.getTitle())
                .description(request.getDescription())
                .startPrice(request.getStartPrice())
                .build();
        itemRepository.save(item);

        addImages(item,request.getImageUrls());
        return item;
    }

    private void addImages(Item item, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        int order = 0;
        for (String url : imageUrls) {
            ItemImage image = ItemImage.builder()
                    .item(item)
                    .imageUrl(url)
                    .sortOrder(order++)
                    .build();

            itemImageRepository.save(image);
            item.addImage(image);

            if (image.getSortOrder() != null && image.getSortOrder() == 0) {
                item.setThumbnailUrl(url);
            }
        }
    }
}
