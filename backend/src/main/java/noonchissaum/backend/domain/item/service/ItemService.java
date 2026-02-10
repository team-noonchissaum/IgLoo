package noonchissaum.backend.domain.item.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.dto.req.AuctionRegisterReq;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.entity.ItemImage;
import noonchissaum.backend.domain.item.repository.ItemImageRepository;
import noonchissaum.backend.domain.item.repository.ItemRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.dto.LocationSearchResult;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;

    public Item getActiveById(Long itemId) {
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

        addImages(item, request.getImageUrls());
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


    /**
     * 위치 기반 검색 (고정 마진 포함)
     */
    public List<Item> searchItemsByLocation(Double latitude, Double longitude, Double radiusKm) {

        if (latitude == null || longitude == null || radiusKm == null) {
            throw new ApiException(ErrorCode.INVALID_LOCATION_PARAMS);
        }

        Double searchRadius = calculateSearchRadius(radiusKm);
        Double radiusInMeters = searchRadius * 1000;

        return itemRepository.findItemsWithinRadius(latitude, longitude, radiusInMeters);
    }

    /**
     * 거리 정보 포함한 검색 (고정 마진 포함)
     */
    public List<LocationSearchResult> searchItemsByLocationWithDistance(
            Double latitude, Double longitude, Double radiusKm) {


        if (latitude == null || longitude == null || radiusKm == null) {
            throw new ApiException(ErrorCode.INVALID_LOCATION_PARAMS);
        }

        Double searchRadius = calculateSearchRadius(radiusKm);
        Double radiusInMeters = searchRadius * 1000;

        List<Map<String, Object>> rawResults =
                itemRepository.findItemsWithDistance(latitude, longitude, radiusInMeters);

        return rawResults.stream()
                .map(result -> buildLocationSearchResult(result, radiusKm))
                .collect(Collectors.toList());
    }

    /**
     * 고정 마진 설정 (0.4km)
     */
    private Double calculateSearchRadius(Double requestedRadius) {
        final Double MARGIN_KM = 0.4;
        return requestedRadius + MARGIN_KM;
    }

    /**
     * 검색 결과를 LocationSearchResult로 변환
     */
    private LocationSearchResult buildLocationSearchResult(
            Map<String, Object> result, Double requestedRadius) {

        Long itemId = ((Number) result.get("item_id")).longValue();
        String title = (String) result.get("title");
        Double distance = ((Number) result.get("distance_km")).doubleValue();
        String sellerDong = (String) result.get("seller_dong");

        String reliability = getReliabilityLabel(distance, requestedRadius);
        String reliabilityIcon = getReliabilityIcon(distance, requestedRadius);

        return LocationSearchResult.builder()
                .itemId(itemId)
                .title(title)
                .distance(distance)
                .sellerDong(sellerDong)
                .reliability(reliability)
                .reliabilityIcon(reliabilityIcon)
                .build();
    }

    /**
     * 거리 기반 신뢰도 레이블
     */
    private String getReliabilityLabel(Double distance, Double requestedRadius) {
        if (distance <= requestedRadius) {
            return "범위 내";
        } else if (distance <= requestedRadius + 0.2) {
            return "거의 범위 내";
        } else {
            return "약간 벗어남";
        }
    }

    /**
     * 신뢰도 아이콘
     */
    private String getReliabilityIcon(Double distance, Double requestedRadius) {
        if (distance <= requestedRadius) {
            return "✓";
        } else if (distance <= requestedRadius + 0.2) {
            return "≈";
        } else {
            return "△";
        }
    }

    /**
     * 사용자 위치 변경 시 기존 매물 위치도 업데이트- 주소를 변경시 해당 판매자 모든 활성 물품도 새로운 위치로 업데이트
     */
    public void updateSellerItemLocations(User seller) {
        List<Item> items = itemRepository.findBySellerId(seller.getId());

        for (Item item : items) {
            if (item.isActive()) {
                item.updateSellerLocation(seller.getLocation(), seller.getDong());
            }
        }

        itemRepository.saveAll(items);
    }

}
