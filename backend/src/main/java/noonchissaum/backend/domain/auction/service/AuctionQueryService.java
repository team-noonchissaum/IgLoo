package noonchissaum.backend.domain.auction.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.dto.res.AuctionListRes;
import noonchissaum.backend.domain.auction.dto.ws.AuctionSnapshotPayload;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionSortType;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.item.service.WishService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionQueryService {

    private final AuctionRepository auctionRepository;
    private final AuctionPriceIndexReadService priceIndexReadService;
    private final AuctionRealtimeSnapshotService snapshotService;
    private final WishService wishService;

    /**
     * 카테고리 + PRICE_HIGH/PRICE_LOW 정렬을 "페이지까지 정확하게" 반환
     * - 정렬/페이지네이션: Redis ZSET
     */
    public Page<AuctionListRes> searchByCategoryPriceSorted(
            Long userId,
            Long categoryId,
            AuctionSortType sort,
            int page,
            int size
    ) {
        if (categoryId == null || sort == null || !sort.isRedisPriceSort()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
        }

        List<Long> ids = priceIndexReadService.getAuctionIdsByCategoryPrice(categoryId, sort, page, size);
        long total = priceIndexReadService.countByCategory(categoryId);

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), total);
        }

        List<Auction> auctions = auctionRepository.findByIdIn(ids);

        // 찜여부 계산
        List<Long> itemIds = auctions.stream()
                .map(a -> a.getItem().getId())
                .distinct()
                .toList();
        Set<Long> wishedItemIds = wishService.getWishedItemIds(userId, itemIds);

        Map<Long, Auction> map = auctions.stream()
                .collect(Collectors.toMap(Auction::getId, Function.identity(), (a, b) -> a));

        List<AuctionListRes> content = new ArrayList<>(ids.size());

        for (Long auctionId : ids) {
            Auction auction = map.get(auctionId);
            if (auction == null) continue;

            boolean isWished = wishedItemIds.contains(auction.getItem().getId());

            AuctionListRes base = AuctionListRes.from(auction, isWished);

            Optional<AuctionSnapshotPayload> snapOpt = snapshotService.getSnapshotIfPresent(auctionId);

            AuctionListRes finalRes = snapOpt
                    .map(snap -> base.withRealtime(
                            snap.getCurrentPrice(),
                            snap.getBidCount(),
                            snap.getEndAt()
                    ))
                    .orElse(base);

            content.add(finalRes);
        }

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }
}