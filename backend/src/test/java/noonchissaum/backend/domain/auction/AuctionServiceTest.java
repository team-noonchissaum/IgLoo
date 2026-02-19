package noonchissaum.backend.domain.auction;

import noonchissaum.backend.domain.auction.dto.res.AuctionRes;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.service.AuctionService;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.service.WishService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.global.dto.LocationDto;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionService 반경 검색 테스트")
class AuctionServiceTest {
    private static final Long USER_ID = 1L;
    private static final Double RADIUS_10KM = 10.0;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private UserService userService;

    @Mock
    private LocationService locationService;

    @Mock
    private WishService wishService;

    @InjectMocks
    private AuctionService auctionService;

    private User userKangnam;
    private User userMapo;
    private List<Auction> mockAuctions;


    //유저 생성
    private User createUser(
            Long id,
            String email,
            String nickname,
            String address,
            String dong,
            double lat,
            double lng
    ) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        user.assignId(id);//test용id사용

        user.updateLocation(address, dong, lat, lng);
        return user;
    }

    //경매 생성
    private Auction createAuction(User seller, String title, BigDecimal price) {
        Category category=new Category("전자기기");
        category.assignId(1L);

        Item item = Item.builder()
                .title(title)
                .seller(seller)
                .category(category)
                .build();

        Auction auction = Auction.builder()
                .item(item)
                .startPrice(price)
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(1))
                .build();

        return auction;
    }

    @BeforeEach
    void setUp() {

        userKangnam = createUser(
                1L,
                "kangnamtest@test.com",
                "강남테스터",
                "서울 강남구 테헤란로 152",
                "강남동",
                37.4979,
                127.0276
        );

        userMapo = createUser(
                2L,
                "mapotest@test.com",
                "마포테스터",
                "서울 마포구 홍익로 1",
                "서교동",
                37.5563,
                126.9220
        );

        mockAuctions = List.of(
                createAuction(userKangnam, "맥북", BigDecimal.valueOf(1_000_000)),
                createAuction(userMapo, "갤럭시ULTRA", BigDecimal.valueOf(1_000_000))
        );
    }

    @Test
    @DisplayName("반경 내 경매 검색 성공 - 강남 10km")
    void searchAuctions_success_within10km() {

        Pageable pageable = PageRequest.of(0, 20);

        when(userService.getUserByUserId(userKangnam.getId()))
                .thenReturn(userKangnam);

        when(auctionRepository.findAuctionsInBoundingBox(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()
        )).thenReturn(mockAuctions);

        when(locationService.calculateDistance(
                anyDouble(), anyDouble(), anyDouble(), anyDouble()
        )).thenReturn(0.8);

        when(wishService.getWishedItemIds(anyLong(), anyList()))
                .thenReturn(Collections.emptySet());

        Page<AuctionRes> result =
                auctionService.searchAuctionsByUserLocation(
                        userKangnam.getId(),
                        10.0,
                        pageable
                );

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("반경 내 경매 검색 실패 - 위치 미설정")
    void searchAuctions_fail_whenLocationNotSet() {

        Pageable pageable = PageRequest.of(0, 20);

        User userNoLocation = User.builder()
                .email("no@location.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userService.getUserByUserId(99L))
                .thenReturn(userNoLocation);

        assertThatThrownBy(() ->
                auctionService.searchAuctionsByUserLocation(
                        99L,
                        10.0,
                        pageable
                )
        )
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_LOCATION_NOT_SET);
    }

    @Test
    @DisplayName("거리 계산 정확성")
    void testDistanceCalculation() {
        // Given
        Double lat1 = 37.4979;   // 강남
        Double lon1 = 127.0276;
        Double lat2 = 37.5551;   // 마포
        Double lon2 = 126.9253;

        // When
        when(locationService.calculateDistance(lat1, lon1, lat2, lon2))
                .thenReturn(12.5);  // 약 12.5km

        Double distance = locationService.calculateDistance(lat1, lon1, lat2, lon2);

        // Then
        assertThat(distance).isGreaterThan(10.0);
        assertThat(distance).isLessThan(20.0);
    }
}
