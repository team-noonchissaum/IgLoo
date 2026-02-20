package noonchissaum.backend.domain.auction.service;

import noonchissaum.backend.domain.auction.dto.req.AuctionRegisterReq;
import noonchissaum.backend.domain.auction.dto.res.AuctionRes;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.category.service.CategoryService;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.service.ItemService;
import noonchissaum.backend.domain.item.service.UserViewRedisLogger;
import noonchissaum.backend.domain.item.service.WishService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.recommendation.service.RecommendationService;
import noonchissaum.backend.global.service.LocationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuctionServiceHotDealTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private ItemService itemService;
    @Mock private UserService userService;
    @Mock private CategoryService categoryService;
    @Mock private WishService wishService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private AuctionRedisService auctionRedisService;
    @Mock private AuctionRealtimeSnapshotService snapshotService;
    @Mock private AuctionQueryService auctionQueryService;
    @Mock private AuctionIndexService auctionIndexService;
    @Mock private WalletService walletService;
    @Mock private UserViewRedisLogger userViewRedisLogger;
    @Mock private RecommendationService recommendationService;
    @Mock private LocationService locationService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AuctionService auctionService;

    @Test
    @DisplayName("핫딜 등록 시 isHotDeal=true, 시작시각+7일, 종료시각 = 시작+duration 으로 저장된다")
    void registerHotDeal_savesHotDealWithFixedSchedule() {
        Long adminId = 1L;
        User admin = user(adminId, "admin@test.com", "admin", UserRole.ADMIN);
        Category category = category(11L, "가전");
        Item item = item(101L, admin, category);

        AuctionRegisterReq req = hotDealReq(11L, BigDecimal.valueOf(5000), 3L);

        given(userService.getUserByUserId(adminId)).willReturn(admin);
        given(categoryService.getCategory(11L)).willReturn(category);
        given(itemService.createHotDealItem(admin, category, req)).willReturn(item);

        Long returnedId = 777L;
        given(auctionRepository.save(any(Auction.class))).willAnswer(invocation -> {
            Auction saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", returnedId);
            return saved;
        });

        Long result = auctionService.registerHotDeal(adminId, req);

        assertThat(result).isEqualTo(returnedId);

        ArgumentCaptor<Auction> captor = ArgumentCaptor.forClass(Auction.class);
        verify(auctionRepository).save(captor.capture());

        Auction savedAuction = captor.getValue();
        assertThat(savedAuction.getIsHotDeal()).isTrue();
        assertThat(savedAuction.getStatus()).isEqualTo(AuctionStatus.READY);

        long daysBetween = Duration.between(LocalDateTime.now(), savedAuction.getStartAt()).toDays();
        assertThat(daysBetween).isBetween(6L, 7L);

        long hoursBetween = Duration.between(savedAuction.getStartAt(), savedAuction.getEndAt()).toHours();
        assertThat(hoursBetween).isEqualTo(3L);
    }

    @Test
    @DisplayName("핫딜 취소 시 READY 상태 + 입찰없음이면 CANCELED 처리하고 Redis 정리한다")
    void cancelHotDeal_cancelsAndClearsRedisWhenValid() {
        Long auctionId = 21L;
        Auction auction = auction(auctionId, true, AuctionStatus.READY, 0);

        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));

        auctionService.cancelHotDeal(1L, auctionId);

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELED);
        verify(auctionRedisService).cancelAuction(auctionId);
    }

    @Test
    @DisplayName("핫딜이 아닌 경매 취소 요청은 INVALID_REQUEST 예외")
    void cancelHotDeal_throwsWhenAuctionIsNotHotDeal() {
        Long auctionId = 22L;
        Auction normalAuction = auction(auctionId, false, AuctionStatus.READY, 0);
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(normalAuction));

        assertThatThrownBy(() -> auctionService.cancelHotDeal(1L, auctionId))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(auctionRedisService, never()).cancelAuction(any());
    }

    @Test
    @DisplayName("READY 상태가 아니면 핫딜 취소는 AUCTION_INVALID_STATUS 예외")
    void cancelHotDeal_throwsWhenStatusIsNotReady() {
        Long auctionId = 23L;
        Auction runningHotDeal = auction(auctionId, true, AuctionStatus.RUNNING, 0);
        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(runningHotDeal));

        assertThatThrownBy(() -> auctionService.cancelHotDeal(1L, auctionId))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUCTION_INVALID_STATUS);
    }

    @Test
    @DisplayName("핫딜 배너 조회는 최대 5개를 조회하고 찜 여부를 반영한다")
    void getHotDeals_returnsWishedInfo() {
        Long userId = 10L;
        Auction a1 = auction(31L, true, AuctionStatus.READY, 0);
        Auction a2 = auction(32L, true, AuctionStatus.READY, 0);

        given(auctionRepository.findHotDeals(any(LocalDateTime.class), any(LocalDateTime.class), eq(PageRequest.of(0, 5))))
                .willReturn(List.of(a1, a2));

        Long wishedItemId = a2.getItem().getId();
        given(wishService.getWishedItemIds(eq(userId), any())).willReturn(Set.of(wishedItemId));

        List<AuctionRes> result = auctionService.getHotDeals(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getIsWished()).isFalse();
        assertThat(result.get(1).getIsWished()).isTrue();
    }

    private AuctionRegisterReq hotDealReq(Long categoryId, BigDecimal startPrice, Long duration) {
        AuctionRegisterReq req = new AuctionRegisterReq();
        ReflectionTestUtils.setField(req, "categoryId", categoryId);
        ReflectionTestUtils.setField(req, "startPrice", startPrice);
        ReflectionTestUtils.setField(req, "auctionDuration", duration);
        ReflectionTestUtils.setField(req, "title", "핫딜 상품");
        ReflectionTestUtils.setField(req, "description", "설명");
        return req;
    }

    private User user(Long id, String email, String nickname, UserRole role) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .role(role)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Category category(Long id, String name) {
        Category category = new Category(name, null);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private Item item(Long itemId, User seller, Category category) {
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title("title")
                .description("desc")
                .startPrice(BigDecimal.valueOf(1000))
                .build();
        ReflectionTestUtils.setField(item, "id", itemId);
        return item;
    }

    private Auction auction(Long auctionId, boolean isHotDeal, AuctionStatus status, Integer bidCount) {
        User seller = user(100L + auctionId, "seller@test.com", "seller" + auctionId, UserRole.USER);
        User bidder = user(200L + auctionId, "buyer@test.com", "buyer" + auctionId, UserRole.USER);
        Category category = category(300L + auctionId, "카테고리" + auctionId);
        Item item = item(400L + auctionId, seller, category);

        Auction auction = Auction.builder()
                .item(item)
                .startPrice(BigDecimal.valueOf(1000))
                .startAt(LocalDateTime.now().plusDays(1))
                .endAt(LocalDateTime.now().plusDays(1).plusHours(2))
                .isHotDeal(isHotDeal)
                .build();

        ReflectionTestUtils.setField(auction, "id", auctionId);
        ReflectionTestUtils.setField(auction, "status", status);
        ReflectionTestUtils.setField(auction, "bidCount", bidCount);
        ReflectionTestUtils.setField(auction, "currentBidder", bidder);
        return auction;
    }
}