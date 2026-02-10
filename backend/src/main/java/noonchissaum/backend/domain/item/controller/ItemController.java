package noonchissaum.backend.domain.item.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.service.ItemService;
import noonchissaum.backend.global.dto.LocationSearchResult;
import noonchissaum.backend.global.dto.SearchItemsRes;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    /**
     * 위치 기반 아이템 검색 (거리 정보 포함)
     * GET /api/items/search?latitude=37.4979&longitude=127.0276&radiusKm=2.0
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchNearby(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radiusKm) {

        // 입력값 검증
        if (latitude == null || latitude < -90 || latitude > 90) {
            throw new ApiException(ErrorCode.INVALID_LATITUDE);
        }
        if (longitude == null || longitude < -180 || longitude > 180) {
            throw new ApiException(ErrorCode.INVALID_LONGITUDE);
        }
        if (radiusKm == null || radiusKm <= 0 || radiusKm > 50) {
            throw new ApiException(ErrorCode.INVALID_RADIUS);
        }

        // 위치 기반 검색
        List<LocationSearchResult> results =
                itemService.searchItemsByLocationWithDistance(latitude, longitude, radiusKm);

        Double searchedRadius = calculateSearchRadius(radiusKm);

        SearchItemsRes response = SearchItemsRes.builder()
                .requestedRadius(radiusKm)
                .searchedRadius(searchedRadius)
                .count(results.size())
                .items(results)
                .message(buildSearchMessage(radiusKm, results.size()))
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 위치 기반 아이템 검색 (빠른 버전 - 거리 정보 없음)
     */
    @GetMapping("/search-fast")
    public ResponseEntity<?> searchNearbyFast(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radiusKm) {
        if (latitude == null || latitude < -90 || latitude > 90) {
            throw new ApiException(ErrorCode.INVALID_LATITUDE);
        }
        if (longitude == null || longitude < -180 || longitude > 180) {
            throw new ApiException(ErrorCode.INVALID_LONGITUDE);
        }
        if (radiusKm == null || radiusKm <= 0 || radiusKm > 50) {
            throw new ApiException(ErrorCode.INVALID_RADIUS);
        }

        List<Item> items = itemService.searchItemsByLocation(latitude, longitude, radiusKm);

        return ResponseEntity.ok(items);
    }

    /**
     * 실제 검색 반경 계산 (고정 마진 0.4km)
     */
    private Double calculateSearchRadius(Double requestedRadius) {
        return requestedRadius + 0.4;
    }

    /**
     * 검색 결과 메시지 생성
     */
    private String buildSearchMessage(Double radiusKm, Integer resultCount) {
        if (resultCount == 0) {
            return String.format("반경 %.1fkm 내에서 찾을 수 있는 아이템이 없습니다.", radiusKm);
        }
        return String.format("반경 %.1fkm 내에서 %d개의 아이템을 찾았습니다.", radiusKm, resultCount);
    }
}
