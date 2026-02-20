package noonchissaum.backend.domain.auction.service.integration;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
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
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Tag("integration")
class BidServiceFlowIntegrationTest {

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

    private User testUser;
    private Auction testAuction;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        testUser = User.builder()
                .nickname("bid_flow_user_" + suffix)
                .email("bidflow_" + suffix + "@example.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(testUser);

        Wallet wallet = Wallet.builder()
                .user(testUser)
                .balance(new BigDecimal("200000"))
                .lockedBalance(BigDecimal.ZERO)
                .build();
        walletRepository.save(wallet);
        testUser.registerWallet(wallet);

        Category category = categoryRepository.save(new Category("flow-cat-" + suffix, null));
        Item item = itemRepository.save(new Item(testUser, category, "flow-item-" + suffix, "desc"));
        testAuction = Auction.builder()
                .item(item)
                .startPrice(new BigDecimal("1000"))
                .startAt(LocalDateTime.now().minusMinutes(10))
                .endAt(LocalDateTime.now().plusHours(1))
                .build();
        testAuction.run();
        auctionRepository.save(testAuction);

        redisTemplate.opsForValue().set("auction:" + testAuction.getId() + ":currentPrice", "1000");
        redisTemplate.opsForValue().set("auction:" + testAuction.getId() + ":currentBidder", "");
        redisTemplate.opsForValue().set("auction:" + testAuction.getId() + ":currentBidCount", "0");
        redisTemplate.opsForValue().set("auction:" + testAuction.getId() + ":status", AuctionStatus.RUNNING.name());
        redisTemplate.opsForValue().set("auction:" + testAuction.getId() + ":endTime", testAuction.getEndAt().toString());
        redisTemplate.opsForValue().set("auction:" + testAuction.getId() + ":extendedTime", "0");
        redisTemplate.opsForValue().set("user:" + testUser.getId() + ":balance", wallet.getBalance().toString());
    }

    @Test
    @DisplayName("단일 사용자 입찰 성공")
    void placeBid_success_singleUser() {
        BigDecimal bidAmount = new BigDecimal("5000");
        String requestId = UUID.randomUUID().toString();

        bidService.placeBid(testAuction.getId(), testUser.getId(), bidAmount, requestId);

        Auction finalAuction = auctionRepository.findById(testAuction.getId()).orElseThrow();
        Bid finalBid = bidRepository.findByAuctionAndBidder(finalAuction, testUser).orElseThrow();
        Wallet finalWallet = walletRepository.findByUserId(testUser.getId()).orElseThrow();
        BigDecimal finalPriceInRedis = new BigDecimal(redisTemplate.opsForValue().get("auction:" + testAuction.getId() + ":currentPrice"));

        assertThat(finalBid.getBidPrice()).isEqualByComparingTo(bidAmount);
        assertThat(finalPriceInRedis).isEqualByComparingTo(bidAmount);
        assertThat(finalWallet.getLockedBalance()).isEqualByComparingTo(bidAmount);
    }

    @Test
    @DisplayName("최소 입찰 단위 미충족 시 예외 던짐")
    void placeBid_fail_lowBidAmount() {
        redisTemplate.opsForValue().set("auction:" + testAuction.getId() + ":currentBidCount", "1");
        BigDecimal bidAmount = new BigDecimal("1050");
        String requestId = UUID.randomUUID().toString();

        ApiException exception = assertThrows(ApiException.class,
                () -> bidService.placeBid(testAuction.getId(), testUser.getId(), bidAmount, requestId));
        assertEquals(ErrorCode.LOW_BID_AMOUNT.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("최고 입찰자 연속 입찰 시 예외 던짐")
    void placeBid_fail_cannotBidContinuous() {
        bidService.placeBid(testAuction.getId(), testUser.getId(), new BigDecimal("2000"), UUID.randomUUID().toString());

        ApiException exception = assertThrows(ApiException.class,
                () -> bidService.placeBid(testAuction.getId(), testUser.getId(), new BigDecimal("3000"), UUID.randomUUID().toString()));
        assertEquals(ErrorCode.CANNOT_BID_CONTINUOUS.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("경매 상태가 RUNNING이 아닐 때 입찰 예외 던짐")
    void placeBid_fail_auctionNotRunning() {
        testAuction.cancel();
        auctionRepository.save(testAuction);
        redisTemplate.opsForValue().set("auction:" + testAuction.getId() + ":status", AuctionStatus.CANCELED.name());

        ApiException exception = assertThrows(ApiException.class,
                () -> bidService.placeBid(testAuction.getId(), testUser.getId(), new BigDecimal("2000"), UUID.randomUUID().toString()));
        assertEquals(ErrorCode.NOT_FOUND_AUCTIONS.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("잔액 부족 시 입찰 예외 던짐")
    void placeBid_fail_insufficientBalance() {
        Wallet wallet = walletRepository.findByUserId(testUser.getId()).orElseThrow();
        wallet.setBalance(new BigDecimal("10000"));
        walletRepository.save(wallet);
        redisTemplate.opsForValue().set("user:" + testUser.getId() + ":balance", "10000");
        ApiException exception = assertThrows(ApiException.class,
                () -> bidService.placeBid(testAuction.getId(), testUser.getId(), new BigDecimal("20000"), UUID.randomUUID().toString()));
        assertEquals(ErrorCode.INSUFFICIENT_BALANCE.getMessage(), exception.getMessage());
    }
}

