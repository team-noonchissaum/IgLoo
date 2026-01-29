package noonchissaum.backend.domain.auction.service;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@TestPropertySource(locations = "file:C:/exam/finalprojects/IgLoo/.env.dev")
public class BidServiceValidationTest {

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
    private StringRedisTemplate redisTemplate;

    private User testUser;
    private Auction testAuction;

    @BeforeEach
    void setUp() {
        bidRepository.deleteAllInBatch();
        auctionRepository.deleteAllInBatch();
        walletRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // Clear Redis
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        testUser = User.builder()
                .nickname("test_user")
                .email("testuser@example.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(testUser);

        Wallet testWallet = Wallet.builder()
                .user(testUser)
                .balance(new BigDecimal("10000"))
                .lockedBalance(new BigDecimal("0"))
                .build();
        walletRepository.save(testWallet);
        testUser.setWallet(testWallet);


        testAuction = Auction.builder()
                .status(AuctionStatus.RUNNING)
                .currentPrice(new BigDecimal("1000"))
                .endAt(LocalDateTime.now().plusHours(1))
                .build();
        auctionRepository.save(testAuction);

        // Redis setup
        String priceKey = "auction:" + testAuction.getId() + ":currentPrice";
        String bidderKey = "auction:" + testAuction.getId() + ":currentBidder";
        String bidCountKey = "auction:" + testAuction.getId() + ":currentBidCount";
        String endTimeKey = "auction:" + testAuction.getId() + ":endTime";
        String extendedTimeKey = "auction:" + testAuction.getId() + ":extendedTime";
        String userBalanceKey = "user:" + testUser.getId() + ":balance";


        redisTemplate.opsForValue().set(priceKey, testAuction.getCurrentPrice().toString());
        redisTemplate.opsForValue().set(bidderKey, "");
        redisTemplate.opsForValue().set(bidCountKey, "0");
        redisTemplate.opsForValue().set(endTimeKey, testAuction.getEndAt().toString());
        redisTemplate.opsForValue().set(extendedTimeKey, "0");
        redisTemplate.opsForValue().set(userBalanceKey, testWallet.getBalance().toString());
    }

    @Test
    @DisplayName("입찰 금액이 현재가보다 10% 이상 높지 않으면 실패")
    void placeBid_Fail_LowBidAmount() {
        // given
        BigDecimal bidAmount = new BigDecimal("1050"); // 현재가 1000원 -> 최소 입찰가 1100원
        String requestId = UUID.randomUUID().toString();

        // when & then
        ApiException exception = assertThrows(ApiException.class, () -> {
            bidService.placeBid(testAuction.getId(), testUser.getId(), bidAmount, requestId);
        });
        assertEquals(ErrorCode.LOW_BID_AMOUNT.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("자신이 최고 입찰자일 때 연속으로 입찰하면 실패")
    void placeBid_Fail_CannotBidContinuous() {
        // given
        // 첫 번째 입찰 (성공)
        BigDecimal firstBidAmount = new BigDecimal("2000");
        String firstRequestId = UUID.randomUUID().toString();
        bidService.placeBid(testAuction.getId(), testUser.getId(), firstBidAmount, firstRequestId);


        // 두 번째 입찰 (실패해야 함)
        BigDecimal secondBidAmount = new BigDecimal("3000");
        String secondRequestId = UUID.randomUUID().toString();

        // when & then
        ApiException exception = assertThrows(ApiException.class, () -> {
            bidService.placeBid(testAuction.getId(), testUser.getId(), secondBidAmount, secondRequestId);
        });
        assertEquals(ErrorCode.CANNOT_BID_CONTINUOUS.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("경매가 RUNNING 상태가 아니면 입찰 실패")
    void placeBid_Fail_AuctionNotRunning() {
        // given
        testAuction.setStatus(AuctionStatus.ENDED);
        auctionRepository.save(testAuction);

        BigDecimal bidAmount = new BigDecimal("2000");
        String requestId = UUID.randomUUID().toString();

        // when & then
        ApiException exception = assertThrows(ApiException.class, () -> {
            bidService.placeBid(testAuction.getId(), testUser.getId(), bidAmount, requestId);
        });
        assertEquals(ErrorCode.NOT_FOUND_AUCTIONS.getMessage(), exception.getMessage());
    }

    @Test
    @DisplayName("사용자 잔액이 입찰 금액보다 적으면 실패")
    void placeBid_Fail_InsufficientBalance() {
        // given
        BigDecimal bidAmount = new BigDecimal("20000"); // 초기 잔액 10000
        String requestId = UUID.randomUUID().toString();

        // when & then
        ApiException exception = assertThrows(ApiException.class, () -> {
            bidService.placeBid(testAuction.getId(), testUser.getId(), bidAmount, requestId);
        });
        assertEquals(ErrorCode.INSUFFICIENT_BALANCE.getMessage(), exception.getMessage());
    }
}
