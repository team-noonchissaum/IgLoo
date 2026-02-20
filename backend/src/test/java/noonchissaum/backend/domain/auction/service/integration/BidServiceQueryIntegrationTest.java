package noonchissaum.backend.domain.auction.service.integration;

import jakarta.persistence.EntityManager;
import noonchissaum.backend.domain.auction.dto.res.BidHistoryItemRes;
import noonchissaum.backend.domain.auction.dto.res.MyBidAuctionRes;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.auction.service.AuctionMessageService;
import noonchissaum.backend.domain.auction.service.AuctionRecordService;
import noonchissaum.backend.domain.auction.service.AuctionRedisService;
import noonchissaum.backend.domain.auction.service.BidRecordService;
import noonchissaum.backend.domain.auction.service.BidService;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.notification.service.NotificationService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.domain.wallet.service.WalletService;
import noonchissaum.backend.global.util.UserLockExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BidService.class)
@Tag("integration")
class BidServiceQueryIntegrationTest {

    @Autowired
    EntityManager em;

    @MockitoBean
    RedissonClient redissonClient;
    @MockitoBean
    StringRedisTemplate stringRedisTemplate;
    @MockitoBean
    WalletService walletService;
    @MockitoBean
    BidRecordService bidRecordService;
    @MockitoBean
    AuctionRedisService auctionRedisService;
    @MockitoBean
    AuctionMessageService auctionMessageService;
    @MockitoBean
    AuctionRecordService auctionRecordService;
    @MockitoBean
    NotificationService notificationService;
    @MockitoBean
    UserLockExecutor userLockExecutor;
    @MockitoBean
    UserService userService;
    @MockitoBean
    ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    BidService bidService;

    private User u1;
    private User u2;
    private Auction a1;
    private Auction a2;
    private Auction a3;

    @BeforeEach
    void setUp() {
        u1 = User.builder()
                .email("u1@test.com")
                .nickname("u1")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        u2 = User.builder()
                .email("u2@test.com")
                .nickname("u2")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        em.persist(u1);
        em.persist(u2);

        Category c1 = new Category("C1", null);
        em.persist(c1);

        Item i1 = new Item(u1, c1, "I1", "desc-1", BigDecimal.valueOf(1000));
        Item i2 = new Item(u1, c1, "I2", "desc-2", BigDecimal.valueOf(1000));
        Item i3 = new Item(u1, c1, "I3", "desc-3", BigDecimal.valueOf(1000));
        em.persist(i1);
        em.persist(i2);
        em.persist(i3);

        LocalDateTime now = LocalDateTime.now();
        a1 = runningAuction(i1, BigDecimal.valueOf(1000), now.plusDays(2));
        a2 = runningAuction(i2, BigDecimal.valueOf(1000), now.plusDays(5));
        a3 = runningAuction(i3, BigDecimal.valueOf(1000), now.plusDays(1));
        em.persist(a1);
        em.persist(a2);
        em.persist(a3);

        em.persist(new Bid(a1, u1, BigDecimal.valueOf(1000), "r-a1-u1-1"));
        em.persist(new Bid(a1, u1, BigDecimal.valueOf(1500), "r-a1-u1-2"));
        em.persist(new Bid(a2, u1, BigDecimal.valueOf(2000), "r-a2-u1-1"));
        em.persist(new Bid(a1, u2, BigDecimal.valueOf(3000), "r-a1-u2-1"));

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("auctionId 기준 입찰 내역 조회 및 DTO 매핑")
    void getBidHistory_success_mapping() {
        Page<BidHistoryItemRes> result = bidService.getBidHistory(a1.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
                .extracting(BidHistoryItemRes::bidderNickname)
                .containsOnly("u1", "u2");
    }

    @Test
    @DisplayName("입찰 내역 조회 시 페이징 적용")
    void getBidHistory_paging() {
        Page<BidHistoryItemRes> result = bidService.getBidHistory(a1.getId(), PageRequest.of(0, 2));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("입찰 내역 2페이지 조회 시 잔여 데이터 반환")
    void getBidHistory_paging_secondPage() {
        Page<BidHistoryItemRes> result = bidService.getBidHistory(a1.getId(), PageRequest.of(1, 2));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("존재하지 않는 auctionId 조회 시 예외 던짐")
    void getBidHistory_notFoundAuction() {
        assertThatThrownBy(() -> bidService.getBidHistory(-999L, PageRequest.of(0, 10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("경매를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("내 참여 경매 조회 및 최고가/입찰수/최고입찰자 여부 계산")
    void getMyBidAuctions_ok() {
        Pageable pageable = PageRequest.of(0, 10, Sort.unsorted());
        Page<MyBidAuctionRes> page = bidService.getMyBidAuctions(u1.getId(), pageable);

        assertEquals(2, page.getTotalElements());
        MyBidAuctionRes first = page.getContent().get(0);
        MyBidAuctionRes second = page.getContent().get(1);

        assertEquals(a2.getId(), first.auctionId());
        assertEquals(a1.getId(), second.auctionId());
        assertEquals(2000L, first.myHighestBidPrice());
        assertEquals(2000L, first.currentPrice());
        assertTrue(first.isHighestBidder());
        assertEquals(1, first.bidCount());
        assertEquals(1500L, second.myHighestBidPrice());
        assertEquals(3000L, second.currentPrice());
        assertFalse(second.isHighestBidder());
        assertEquals(3, second.bidCount());
    }

    @Test
    @DisplayName("PageRequest size가 0 이하일 때 IllegalArgumentException 예외 던짐")
    void pageRequest_invalidSize() {
        assertThrows(IllegalArgumentException.class, () -> PageRequest.of(0, 0));
        assertThrows(IllegalArgumentException.class, () -> PageRequest.of(0, -1));
    }

    @Test
    @DisplayName("PageRequest page가 0 미만일 때 IllegalArgumentException 예외 던짐")
    void pageRequest_invalidPage() {
        assertThrows(IllegalArgumentException.class, () -> PageRequest.of(-1, 10));
    }

    private Auction runningAuction(Item item, BigDecimal startPrice, LocalDateTime endAt) {
        Auction auction = Auction.builder()
                .item(item)
                .startPrice(startPrice)
                .startAt(LocalDateTime.now().minusHours(1))
                .endAt(endAt)
                .build();
        auction.run();
        return auction;
    }
}
