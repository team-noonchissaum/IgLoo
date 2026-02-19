package noonchissaum.backend.domain.auction.service.integration;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.auction.service.BidService;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.category.repository.CategoryRepository;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.repository.ItemRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.domain.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class BidServiceSingleBidTest {

    private static final Logger log = LoggerFactory.getLogger(BidServiceSingleBidTest.class);
    @Autowired
    private BidService bidService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Auction testAuction;
    private User testUser;
    private Wallet testWallet;
    @Autowired
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. 테스트 사용자 및 지갑 데이터 생성
        testUser = User.builder()
                .nickname("single_bid_user_" + suffix)
                .email("singlebid_" + suffix + "@example.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(testUser);

        testWallet = Wallet.builder()
                .user(testUser)
                .balance(new BigDecimal("200000"))
                .lockedBalance(new BigDecimal("0"))
                .build();
        walletRepository.save(testWallet);

        testUser.registerWallet(testWallet);
        
        String userBalanceKey = "user:" + testUser.getId() + ":balance";
        redisTemplate.opsForValue().set(userBalanceKey, testWallet.getBalance().toString());

        // 2. 테스트용 경매 데이터 생성
        Category category = categoryRepository.save(new Category("single-cat-" + suffix, null));
        Item item = itemRepository.save(new Item(testUser, category, "single-bid-item-" + suffix, "desc", new BigDecimal("1000")));

        testAuction = Auction.builder()
                .item(item)
                .startPrice(new BigDecimal("1000"))
                .startAt(LocalDateTime.now().minusMinutes(10))
                .endAt(LocalDateTime.now().plusHours(1))
                .build();
        testAuction.run();
        auctionRepository.save(testAuction);

        // 3. Redis 경매 가격 초기화
        String auctionPriceKey = "auction:" + testAuction.getId() + ":currentPrice";
        redisTemplate.opsForValue().set(auctionPriceKey, testAuction.getCurrentPrice().toString());
    }

    @Test
    @DisplayName("단일 사용자 입찰 성공 테스트")
    void placeBid_Success_SingleUser() {
        String userBalanceKey = "user:" + testUser.getId() + ":balance";
        // given
        BigDecimal bidAmount = new BigDecimal("5000");
        String requestId = UUID.randomUUID().toString();

        // when
        bidService.placeBid(testAuction.getId(), testUser.getId(), bidAmount, requestId);

        // then
        // 1. DB에서 최종 결과 조회
        Auction finalAuction = auctionRepository.findById(testAuction.getId()).orElseThrow();
        Bid finalBid = bidRepository.findByAuctionAndBidder(finalAuction, testUser).orElseThrow();
        Wallet finalWallet = walletRepository.findByUserId(testUser.getId()).orElseThrow();

        // 2. Redis에서 최종 입찰가 조회
        String finalPriceInRedisStr = redisTemplate.opsForValue().get("auction:" + testAuction.getId() + ":currentPrice");
        BigDecimal finalPriceInRedis = new BigDecimal(finalPriceInRedisStr);

        // 3. 결과 검증
        // 3-1. 입찰 기록이 정상적으로 저장되었는가
        assertThat(finalBid).isNotNull();
        assertThat(finalBid.getBidPrice()).isEqualByComparingTo(bidAmount);

        // 3-2. 경매의 현재가가 입찰가로 업데이트되었는가 (DB)
//        assertThat(finalAuction.getCurrentPrice()).isEqualByComparingTo(bidAmount);

        // 3-3. 경매의 현재가가 입찰가로 업데이트되었는가 (Redis)
        assertThat(finalPriceInRedis).isEqualByComparingTo(bidAmount);

        // 3-4. 사용자의 지갑에 입찰 금액만큼 잠긴 잔액(lockedBalance)이 증가했는가
        assertThat(finalWallet.getLockedBalance()).isEqualByComparingTo(bidAmount);
    }
}
