package noonchissaum.backend.domain.auction.service.unit;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.service.AuctionIndexService;
import noonchissaum.backend.domain.auction.service.AuctionQueryService;
import noonchissaum.backend.domain.auction.service.AuctionRealtimeSnapshotService;
import noonchissaum.backend.domain.auction.service.AuctionRedisService;
import noonchissaum.backend.domain.auction.service.AuctionService;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.category.service.CategoryService;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.service.ItemService;
import noonchissaum.backend.domain.item.service.UserViewRedisLogger;
import noonchissaum.backend.domain.item.service.WishService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.service.LocationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuctionServiceUnitTest {

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private ItemService itemService;
    @Mock
    private UserService userService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private WishService wishService;
    @Mock
    private AuctionRedisService auctionRedisService;
    @Mock
    private AuctionRealtimeSnapshotService snapshotService;
    @Mock
    private AuctionQueryService auctionQueryService;
    @Mock
    private AuctionIndexService auctionIndexService;
    @Mock
    private WalletService walletService;
    @Mock
    private UserViewRedisLogger userViewRedisLogger;
    @Mock
    private noonchissaum.backend.recommendation.service.RecommendationService recommendationService;
    @Mock
    private LocationService locationService;
    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("경매 취소 시 생성 5분 이내면 보증금 환불 처리 후 취소")
    void cancelAuction_withinFiveMinutes_refundsDepositAndCancels() {
        AuctionService service = createService();
        Auction auction = sampleAuction(200L, 7L, "cancel-refund");
        ReflectionTestUtils.setField(auction, "createdAt", LocalDateTime.now().minusMinutes(2));
        when(auctionRepository.findById(200L)).thenReturn(Optional.of(auction));

        service.cancelAuction(7L, 200L);

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELED);
        verify(walletService).setAuctionDeposit(7L, 200L, 1000, "refund");
        verify(auctionRedisService).cancelAuction(200L);
    }

    @Test
    @DisplayName("경매 취소 시 판매자가 아니면 AUCTION_NOT_OWNER 예외 던짐")
    void cancelAuction_whenUserIsNotOwner_throwsApiException() {
        AuctionService service = createService();
        Auction auction = sampleAuction(201L, 7L, "cancel-owner");
        when(auctionRepository.findById(201L)).thenReturn(Optional.of(auction));

        ApiException ex = assertThrows(ApiException.class, () -> service.cancelAuction(99L, 201L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUCTION_NOT_OWNER);
    }

    @Test
    @DisplayName("경매 상세 조회 시 차단 상태면 AUCTION_BLOCKED 예외 던짐")
    void getAuctionDetail_whenBlocked_throwsCustomException() {
        AuctionService service = createService();
        Auction auction = sampleAuction(300L, 8L, "detail-blocked");
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.BLOCKED);
        when(auctionRepository.findById(300L)).thenReturn(Optional.of(auction));

        CustomException ex = assertThrows(CustomException.class, () -> service.getAuctionDetail(8L, 300L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUCTION_BLOCKED);
    }

    private AuctionService createService() {
        return new AuctionService(
                auctionRepository,
                itemService,
                userService,
                categoryService,
                wishService,
                auctionRedisService,
                snapshotService,
                auctionQueryService,
                auctionIndexService,
                walletService,
                userViewRedisLogger,
                recommendationService,
                locationService,
                userRepository
        );
    }

    private Auction sampleAuction(Long auctionId, Long sellerId, String suffix) {
        User seller = User.builder()
                .email("auction-service-unit-seller-" + suffix + "@test.com")
                .nickname("auction_service_unit_seller_" + suffix)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(seller, "id", sellerId);

        Category category = new Category("auction-service-category-" + suffix, null);
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title("title-" + suffix)
                .description("desc")
                .startPrice(BigDecimal.valueOf(10000))
                .build();
        ReflectionTestUtils.setField(item, "id", auctionId + 1000);

        Auction auction = Auction.builder()
                .item(item)
                .startPrice(BigDecimal.valueOf(10000))
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(auction, "id", auctionId);
        ReflectionTestUtils.setField(auction, "bidCount", 0);
        return auction;
    }
}
