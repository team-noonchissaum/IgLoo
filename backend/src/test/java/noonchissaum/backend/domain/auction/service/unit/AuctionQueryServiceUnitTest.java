package noonchissaum.backend.domain.auction.service.unit;

import noonchissaum.backend.domain.auction.dto.res.AuctionListRes;
import noonchissaum.backend.domain.auction.dto.ws.AuctionSnapshotPayload;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionSortType;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.service.AuctionPriceIndexReadService;
import noonchissaum.backend.domain.auction.service.AuctionQueryService;
import noonchissaum.backend.domain.auction.service.AuctionRealtimeSnapshotService;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.service.WishService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuctionQueryServiceUnitTest {

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private AuctionPriceIndexReadService priceIndexReadService;
    @Mock
    private AuctionRealtimeSnapshotService snapshotService;
    @Mock
    private WishService wishService;

    @Test
    @DisplayName("카테고리/정렬 파라미터가 유효하지 않으면 빈 페이지 반환")
    void searchByCategoryPriceSorted_withInvalidSort_returnsEmptyPage() {
        AuctionQueryService service = new AuctionQueryService(auctionRepository, priceIndexReadService, snapshotService, wishService);

        Page<AuctionListRes> result = service.searchByCategoryPriceSorted(1L, 1L, AuctionSortType.LATEST, 0, 10);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
        verify(priceIndexReadService, never()).getAuctionIdsByCategoryPrice(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("조회 ID가 없으면 total만 포함한 빈 페이지 반환")
    void searchByCategoryPriceSorted_whenIdsEmpty_returnsEmptyContentWithTotal() {
        AuctionQueryService service = new AuctionQueryService(auctionRepository, priceIndexReadService, snapshotService, wishService);
        when(priceIndexReadService.getAuctionIdsByCategoryPrice(2L, AuctionSortType.PRICE_LOW, 0, 5)).thenReturn(List.of());
        when(priceIndexReadService.countByCategory(2L)).thenReturn(13L);

        Page<AuctionListRes> result = service.searchByCategoryPriceSorted(1L, 2L, AuctionSortType.PRICE_LOW, 0, 5);

        assertThat(result.getTotalElements()).isEqualTo(13L);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("조회 결과는 Redis ID 순서를 유지하고 스냅샷이 있으면 실시간 값으로 덮어씀")
    void searchByCategoryPriceSorted_appliesSnapshotAndMaintainsIdOrder() {
        AuctionQueryService service = new AuctionQueryService(auctionRepository, priceIndexReadService, snapshotService, wishService);
        Auction auction1 = sampleAuction(101L, 1001L, 2001L, "a1", 10000);
        Auction auction2 = sampleAuction(202L, 1002L, 2002L, "a2", 20000);

        when(priceIndexReadService.getAuctionIdsByCategoryPrice(8L, AuctionSortType.PRICE_HIGH, 0, 2)).thenReturn(List.of(202L, 101L));
        when(priceIndexReadService.countByCategory(8L)).thenReturn(2L);
        when(auctionRepository.findByIdIn(List.of(202L, 101L))).thenReturn(List.of(auction1, auction2));
        when(wishService.getWishedItemIds(9L, List.of(1001L, 1002L))).thenReturn(Set.of(1001L));

        AuctionSnapshotPayload snap = new AuctionSnapshotPayload(202L, 77777L, 19L, 6, LocalDateTime.now().plusMinutes(30), 5, true);
        when(snapshotService.getSnapshotIfPresent(202L)).thenReturn(Optional.of(snap));
        when(snapshotService.getSnapshotIfPresent(101L)).thenReturn(Optional.empty());

        Page<AuctionListRes> result = service.searchByCategoryPriceSorted(9L, 8L, AuctionSortType.PRICE_HIGH, 0, 2);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getAuctionId()).isEqualTo(202L);
        assertThat(result.getContent().get(0).getCurrentPrice()).isEqualByComparingTo(BigDecimal.valueOf(77777));
        assertThat(result.getContent().get(0).getBidCount()).isEqualTo(6);

        assertThat(result.getContent().get(1).getAuctionId()).isEqualTo(101L);
        assertThat(result.getContent().get(1).getIsWished()).isTrue();
    }

    private Auction sampleAuction(Long auctionId, Long itemId, Long categoryId, String suffix, int price) {
        User seller = User.builder()
                .email("auction-query-seller-" + suffix + "@test.com")
                .nickname("seller_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(seller, "id", 301L + auctionId);

        Category category = new Category("cat-" + suffix, null);
        ReflectionTestUtils.setField(category, "id", categoryId);

        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title("item-" + suffix)
                .description("desc")
                .build();
        ReflectionTestUtils.setField(item, "id", itemId);

        Auction auction = Auction.builder()
                .item(item)
                .startPrice(BigDecimal.valueOf(price))
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(auction, "id", auctionId);
        return auction;
    }
}

