package noonchissaum.backend.domain.auction.service;

import jakarta.persistence.EntityManager;
import noonchissaum.backend.domain.auction.dto.MyBidAuctionRes;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BidService.class, BidServiceGetMyBidAuctionsTest.TestStubs.class})
class BidServiceGetMyBidAuctionsTest {

    @Autowired EntityManager em;

    @MockitoBean RedissonClient redissonClient;
    @MockitoBean StringRedisTemplate stringRedisTemplate;
    @MockitoBean WalletService walletService;
    @MockitoBean BidRecordService bidRecordService;
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

        // items (seller/category 필수)
        i1 = new Item(u1, c1, "I1", BigDecimal.valueOf(1000));
        i2 = new Item(u1, c1, "I2", BigDecimal.valueOf(1000));
        i3 = new Item(u1, c1, "I3", BigDecimal.valueOf(1000));
        em.persist(i1);
        em.persist(i2);
        em.persist(i3);

        // auctions (builder가 status/currentPrice만 받아서 reflection 주입)
        a1 = Auction.builder().status(AuctionStatus.RUNNING).currentPrice(BigDecimal.ZERO).build();
        a2 = Auction.builder().status(AuctionStatus.RUNNING).currentPrice(BigDecimal.ZERO).build();
        a3 = Auction.builder().status(AuctionStatus.RUNNING).currentPrice(BigDecimal.ZERO).build();

        ReflectionTestUtils.setField(a1, "item", i1);
        ReflectionTestUtils.setField(a2, "item", i2);
        ReflectionTestUtils.setField(a3, "item", i3);

        LocalDateTime now = LocalDateTime.now();
        ReflectionTestUtils.setField(a1, "startAt", now.minusHours(1));
        ReflectionTestUtils.setField(a2, "startAt", now.minusHours(1));
        ReflectionTestUtils.setField(a3, "startAt", now.minusHours(1));

        // 정렬 검증용: endAt 내림차순이면 a2가 먼저
        ReflectionTestUtils.setField(a1, "endAt", now.plusDays(2));
        ReflectionTestUtils.setField(a2, "endAt", now.plusDays(5));
        ReflectionTestUtils.setField(a3, "endAt", now.plusDays(1));

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

    /**
     * BidService는 생성자 주입 필드가 많아서, getMyBidAuctions에서는 안 쓰더라도
     * 스프링이 빈 생성하려면 다 채워줘야 함.
     * 그래서 테스트 전용으로 "더미 빈"을 등록한다.
     *
     * Mockito 없이 가는 A 루트니까, 진짜 동작은 필요 없고 주입만 되면 됨.
     */
    static class TestStubs {

        @Bean
        RedissonClient redissonClient() {
            return null; // getMyBidAuctions에서는 사용 안 함
        }

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return null; // getMyBidAuctions에서는 사용 안 함
        }

        @Bean
        WalletService walletService() {
            return null; // getMyBidAuctions에서는 사용 안 함
        }

        @Bean
        BidRecordService bidRecordService() {
            return null; // getMyBidAuctions에서는 사용 안 함
        }

        @Bean
        UserService userService() {
            return null; // getMyBidAuctions에서는 사용 안 함
        }

        @Bean
        ApplicationEventPublisher applicationEventPublisher() {
            return event -> {}; // noop publisher
        }
    }
}
