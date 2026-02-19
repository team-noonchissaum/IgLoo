package noonchissaum.backend.domain.auction.service.integration;

import jakarta.persistence.EntityManager;
import noonchissaum.backend.domain.auction.dto.res.MyBidAuctionRes;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.notification.service.NotificationService;
import noonchissaum.backend.domain.auction.service.AuctionMessageService;
import noonchissaum.backend.domain.auction.service.AuctionRecordService;
import noonchissaum.backend.domain.auction.service.AuctionRedisService;
import noonchissaum.backend.domain.auction.service.BidRecordService;
import noonchissaum.backend.domain.auction.service.BidService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.domain.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import noonchissaum.backend.global.util.UserLockExecutor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BidService.class)
class BidServiceGetMyBidAuctionsTest {

    @Autowired EntityManager em;

    @MockitoBean RedissonClient redissonClient;
    @MockitoBean StringRedisTemplate stringRedisTemplate;
    @MockitoBean WalletService walletService;
    @MockitoBean BidRecordService bidRecordService;
    @MockitoBean AuctionRedisService auctionRedisService;
    @MockitoBean AuctionMessageService auctionMessageService;
    @MockitoBean AuctionRecordService auctionRecordService;
    @MockitoBean NotificationService notificationService;
    @MockitoBean UserLockExecutor userLockExecutor;
    @MockitoBean UserService userService;
    @MockitoBean ApplicationEventPublisher applicationEventPublisher;

    @Autowired BidService bidService;


    private User u1;
    private User u2;

    private Category c1;

    private Item i1;
    private Item i2;
    private Item i3;

    private Auction a1;
    private Auction a2;
    private Auction a3;

    @BeforeEach
    void setUp() {
        // users
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

        // category
        c1 = new Category("C1", null);
        em.persist(c1);

        // items
        i1 = new Item(u1, c1, "I1", "desc-1", BigDecimal.valueOf(1000));
        i2 = new Item(u1, c1, "I2", "desc-2", BigDecimal.valueOf(1000));
        i3 = new Item(u1, c1, "I3", "desc-3", BigDecimal.valueOf(1000));
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

        // bids:
        // u1: a1에 1000/1500, a2에 2000
        em.persist(new Bid(a1, u1, BigDecimal.valueOf(1000), "r-a1-u1-1"));
        em.persist(new Bid(a1, u1, BigDecimal.valueOf(1500), "r-a1-u1-2"));
        em.persist(new Bid(a2, u1, BigDecimal.valueOf(2000), "r-a2-u1-1"));

        // u2: a1에 3000 (현재 최고가)
        em.persist(new Bid(a1, u2, BigDecimal.valueOf(3000), "r-a1-u2-1"));

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("getMyBidAuctions: 내가 참여한 경매만 나오고(endAt desc), myMax/currentMax/bidCount/isHighest 계산이 맞다")
    void getMyBidAuctions_ok() {
        Pageable pageable = PageRequest.of(0, 10, Sort.unsorted());

        Page<MyBidAuctionRes> page = bidService.getMyBidAuctions(u1.getId(), pageable);

        // u1은 a1, a2 참여 -> 2개
        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getContent().size());

        // 정렬: a2(endAt +5d) -> a1(+2d)
        MyBidAuctionRes first = page.getContent().get(0);
        MyBidAuctionRes second = page.getContent().get(1);

        assertEquals(a2.getId(), first.auctionId());
        assertEquals(a1.getId(), second.auctionId());

        // a2: u1 최고가 2000, 현재 최고가도 2000, 입찰수 1, 최고 입찰자 true
        assertEquals(2000L, first.myHighestBidPrice());
        assertEquals(2000L, first.currentPrice());
        assertTrue(first.isHighestBidder());
        assertEquals(1, first.bidCount());

        // a1: u1 최고가 1500, 현재 최고가 3000(u2), 입찰수 3, 최고 입찰자 false
        assertEquals(1500L, second.myHighestBidPrice());
        assertEquals(3000L, second.currentPrice());
        assertFalse(second.isHighestBidder());
        assertEquals(3, second.bidCount());
    }

    @Test
    @DisplayName("PageRequest가 size<=0이면 생성 시점에 IllegalArgumentException")
    void pageable_invalid_size() {
        assertThrows(IllegalArgumentException.class, () -> PageRequest.of(0, 0));
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
