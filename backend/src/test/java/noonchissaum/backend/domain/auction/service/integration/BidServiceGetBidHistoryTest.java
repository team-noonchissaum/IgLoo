package noonchissaum.backend.domain.auction.service.integration;

import jakarta.persistence.EntityManager;
import noonchissaum.backend.domain.auction.dto.res.BidHistoryItemRes;
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
import org.springframework.context.annotation.Import;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import noonchissaum.backend.global.util.UserLockExecutor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BidService.class)
class BidServiceGetBidHistoryTest {

    @Autowired
    EntityManager em;

    @MockitoBean
    RedissonClient redissonClient;
    @MockitoBean
    StringRedisTemplate stringRedisTemplate;
    @MockitoBean
    WalletService walletService;
    @MockitoBean BidRecordService bidRecordService;
    @MockitoBean AuctionRedisService auctionRedisService;
    @MockitoBean AuctionMessageService auctionMessageService;
    @MockitoBean AuctionRecordService auctionRecordService;
    @MockitoBean NotificationService notificationService;
    @MockitoBean UserLockExecutor userLockExecutor;
    @MockitoBean
    UserService userService;
    @MockitoBean
    ApplicationEventPublisher applicationEventPublisher;

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
    @DisplayName("getBidHistory: auctionId로 입찰 내역이 조회되고 DTO 매핑이 된다")
    void getBidHistory_success_mapping() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);
        Long auctionId = a1.getId();

        // when
        Page<BidHistoryItemRes> result = bidService.getBidHistory(auctionId, pageable);
        List<BidHistoryItemRes> content = result.getContent();

        // then
        assertThat(content).hasSize(3);

        // DTO 매핑 검증
        assertThat(content).allSatisfy(it -> {
            assertThat(it.bidId()).isNotNull();
            assertThat(it.bidderNickname()).isNotBlank();
            assertThat(it.bidPrice()).isNotNull();
            assertThat(it.createdAt()).isNotNull();
        });

        // bidderNickname이 실제 유저 닉네임으로 매핑되는지 (u1/u2 중 하나)
        assertThat(content)
                .extracting(BidHistoryItemRes::bidderNickname)
                .containsOnly("u1", "u2");
    }

    @Test
    @DisplayName("getBidHistory: 페이징이 적용된다 (size=2면 2개만 조회)")
    void getBidHistory_paging() {
        // given
        PageRequest pageable = PageRequest.of(0, 2);
        Long auctionId = a1.getId();

        // when
        Page<BidHistoryItemRes> result = bidService.getBidHistory(auctionId, pageable);

        // then
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("getBidHistory: 2페이지(page=1) 조회하면 나머지 1개가 나온다")
    void getBidHistory_paging_secondPage() {
        // given
        PageRequest pageable = PageRequest.of(1, 2);
        Long auctionId = a1.getId();

        // when
        Page<BidHistoryItemRes> result = bidService.getBidHistory(auctionId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("getBidHistory: 존재하지 않는 auctionId면 예외")
    void getBidHistory_notFoundAuction() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);

        // when & then
        assertThatThrownBy(() -> bidService.getBidHistory(-999L, pageable))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("경매를 찾을 수 없습니다."); // ErrorCode 메시지 실제 문자열에 맞춰 수정
    }

    @Test
    @DisplayName("PageRequest: size<=0이면 PageRequest 생성에서 IllegalArgumentException")
    void pageRequest_invalidSize() {
        assertThatThrownBy(() -> PageRequest.of(0, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PageRequest.of(0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("PageRequest: page<0이면 PageRequest 생성에서 IllegalArgumentException")
    void pageRequest_invalidPage() {
        assertThatThrownBy(() -> PageRequest.of(-1, 10))
                .isInstanceOf(IllegalArgumentException.class);
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
