package noonchissaum.backend.domain.auction.repository;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BidRepositoryDataJpaTest {

    @Autowired EntityManager em;
    @Autowired BidRepository bidRepository;
    @Autowired AuctionRepository auctionRepository;

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
        // ---------- Users ----------
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

        // ---------- Category ----------
        c1 = new Category("C1", null);
        em.persist(c1);

        // ---------- Items (seller/category/title/startPrice 필수) ----------
        i1 = new Item(u1, c1, "I1", BigDecimal.valueOf(1000));
        i2 = new Item(u1, c1, "I2", BigDecimal.valueOf(1000));
        i3 = new Item(u1, c1, "I3", BigDecimal.valueOf(1000));
        em.persist(i1);
        em.persist(i2);
        em.persist(i3);

        // ---------- Auctions ----------
        // Auction은 Builder가 status/currentPrice만 받으므로, 나머지는 Reflection으로 세팅
        a1 = Auction.builder()
                .status(AuctionStatus.RUNNING)
                .currentPrice(BigDecimal.ZERO)
                .build();

        a2 = Auction.builder()
                .status(AuctionStatus.RUNNING)
                .currentPrice(BigDecimal.ZERO)
                .build();

        a3 = Auction.builder()
                .status(AuctionStatus.RUNNING)
                .currentPrice(BigDecimal.ZERO)
                .build();

        // item 연결 (Auction이 owning side: @JoinColumn(item_id))
        ReflectionTestUtils.setField(a1, "item", i1);
        ReflectionTestUtils.setField(a2, "item", i2);
        ReflectionTestUtils.setField(a3, "item", i3);

        // startAt/endAt 세팅 (정렬 검증용으로 endAt 값 다르게)
        LocalDateTime now = LocalDateTime.now();
        ReflectionTestUtils.setField(a1, "startAt", now.minusHours(1));
        ReflectionTestUtils.setField(a2, "startAt", now.minusHours(1));
        ReflectionTestUtils.setField(a3, "startAt", now.minusHours(1));

        ReflectionTestUtils.setField(a1, "endAt", now.plusDays(2)); // 중간
        ReflectionTestUtils.setField(a2, "endAt", now.plusDays(5)); // 가장 늦게 끝남 (desc 정렬에서 1등)
        ReflectionTestUtils.setField(a3, "endAt", now.plusDays(1)); // 가장 빨리 끝남

        em.persist(a1);
        em.persist(a2);
        em.persist(a3);

        // ---------- Bids ----------
        // U1이 A1에 2번 입찰 (중복 제거 + myMax 검증)
        em.persist(new Bid(a1, u1, BigDecimal.valueOf(1000), "r-a1-u1-1"));
        em.persist(new Bid(a1, u1, BigDecimal.valueOf(1500), "r-a1-u1-2"));

        // U1이 A2에 1번 입찰
        em.persist(new Bid(a2, u1, BigDecimal.valueOf(2000), "r-a2-u1-1"));

        // U2가 A1에 더 높은 값 입찰 (currentMax 검증)
        em.persist(new Bid(a1, u2, BigDecimal.valueOf(3000), "r-a1-u2-1"));

        // flush/clear로 “진짜 DB에서 다시 읽게” 만들기 (영속성 컨텍스트 착시 방지)
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findParticipatedAuctions: 유저가 입찰한 경매만 조회되고, endAt desc 정렬 + distinct가 적용된다")
    void findParticipatedAuctions_distinct_and_order() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Auction> page = bidRepository.findParticipatedAuctions(u1.getId(), pageable);
        List<Auction> content = page.getContent();

        // U1은 A1, A2에만 입찰했으니 2개
        assertEquals(2, content.size());

        // endAt desc: A2(endAt +5d) 가 A1(+2d) 보다 먼저
        assertEquals(a2.getId(), content.get(0).getId());
        assertEquals(a1.getId(), content.get(1).getId());

        // distinct 검증: A1에 U1이 2번 입찰했어도 A1은 1번만 나와야 함
        long countA1 = content.stream().filter(a -> a.getId().equals(a1.getId())).count();
        assertEquals(1, countA1);

        // 입찰 없는 A3는 포함되면 안 됨
        boolean containsA3 = content.stream().anyMatch(a -> a.getId().equals(a3.getId()));
        assertFalse(containsA3);
    }

    @Test
    @DisplayName("myMaxBid: 유저-경매별 최고 입찰가가 맞고, 입찰이 없으면 0을 반환한다(coalesce)")
    void myMaxBid_returns_max_or_zero() {
        BigDecimal maxA1 = bidRepository.myMaxBid(u1.getId(), a1.getId());
        assertEquals(0, maxA1.compareTo(BigDecimal.valueOf(1500)));

        BigDecimal noneA3 = bidRepository.myMaxBid(u1.getId(), a3.getId());
        assertEquals(0, noneA3.compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("currentMaxBid: 경매 전체 최고 입찰가가 맞고, 입찰이 없으면 0을 반환한다(coalesce)")
    void currentMaxBid_returns_max_or_zero() {
        BigDecimal maxA1 = bidRepository.currentMaxBid(a1.getId());
        assertEquals(0, maxA1.compareTo(BigDecimal.valueOf(3000)));

        BigDecimal noneA3 = bidRepository.currentMaxBid(a3.getId());
        assertEquals(0, noneA3.compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("findByAuctionIdOrderByCreatedAtDesc: createdAt desc 정렬 + 페이징이 적용된다")
    void findByAuctionIdOrderByCreatedAtDesc_paging() {
        // A1에는 총 3개 bid가 들어가 있음 (u1 2개 + u2 1개)
        Pageable firstPage = PageRequest.of(0, 2);
        Page<Bid> p1 = bidRepository.findByAuctionIdOrderByCreatedAtDesc(a1.getId(), firstPage);

        assertEquals(2, p1.getContent().size());
        assertEquals(3, p1.getTotalElements());

        // 두 번째 페이지 (나머지 1개)
        Pageable secondPage = PageRequest.of(1, 2);
        Page<Bid> p2 = bidRepository.findByAuctionIdOrderByCreatedAtDesc(a1.getId(), secondPage);

        assertEquals(1, p2.getContent().size());

        // createdAt desc가 실제로 적용되는지는 createdAt 값 비교로도 볼 수 있음
        // (BaseTimeEntity 구현에 따라 createdAt이 null이 아닐 때만 의미 있음)
        Bid first = p1.getContent().get(0);
        Bid second = p1.getContent().get(1);
        assertNotNull(first.getCreatedAt());
        assertNotNull(second.getCreatedAt());
        assertTrue(first.getCreatedAt().isAfter(second.getCreatedAt()) || first.getCreatedAt().isEqual(second.getCreatedAt()));
    }
}
