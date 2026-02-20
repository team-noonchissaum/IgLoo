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
import noonchissaum.backend.global.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("integration")
public class BidServiceConcurrencyTest {

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
    private final List<User> testUsers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        testUsers.clear();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. 100명의 테스트 사용자 및 지갑 데이터 생성
        for (int i = 0; i < 100; i++) {
            User user = User.builder()
                    .nickname("concurrent_user_" + suffix + "_" + i)
                    .email("concurrent_" + suffix + "_" + i + "@example.com")
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();
            testUsers.add(user);
            User save = userRepository.save(user);
            Long id = save.getId();
            String userBalance = "user:" + id + ":balance";


            Wallet wallet = walletRepository.save(Wallet.builder()
                    .user(save)
                    .balance(new BigDecimal("200000")) // 모든 사용자가 입찰 가능한 충분한 잔액
                    .lockedBalance(new BigDecimal("0"))
                    .build());

            redisTemplate.opsForValue().set(userBalance,wallet.getBalance().toString());
        }


        // 2. 테스트용 경매 데이터 생성
        User seller = testUsers.get(0);
        Category category = categoryRepository.save(new Category("ccat-" + suffix, null));
        Item item = itemRepository.save(new Item(seller, category, "concurrency-item-" + suffix, "desc", new BigDecimal("1000")));

        testAuction = Auction.builder()
                .item(item)
                .startPrice(new BigDecimal("1000"))
                .startAt(LocalDateTime.now().minusMinutes(10))
                .endAt(LocalDateTime.now().plusHours(1))
                .build();
        testAuction.run();
        auctionRepository.save(testAuction);

        // 3. Redis 초기화

        redisTemplate.opsForValue().set("auction:" + testAuction.getId() + ":currentPrice", testAuction.getCurrentPrice().toString());
    }

    @Test
    @DisplayName("동시 입찰 100건 처리 시 Redis 최종가와 DB 최고가 동기화 검증")
    void 백명이_동시에_입찰하는_상황_테스트() throws InterruptedException {
        // given
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);
        AtomicInteger failureCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < numberOfThreads; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    User currentUser = testUsers.get(userIndex);
                    BigDecimal bidAmount = new BigDecimal("10000").add(new BigDecimal(userIndex * 10));
                    String requestId = UUID.randomUUID().toString();

                    bidService.placeBid(testAuction.getId(), currentUser.getId(), bidAmount, requestId);

                } catch (ApiException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("예상치 못한 예외: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 모든 스레드의 작업이 완료될 때까지 대기
        doneLatch.await(1, TimeUnit.MINUTES);
        executorService.shutdown();

        // then: 결과 검증
        // 1. Redis에서 최종 입찰가 조회
        String finalPriceInRedisStr = redisTemplate.opsForValue().get("auction:" + testAuction.getId() + ":currentPrice");
        BigDecimal finalPriceInRedis = new BigDecimal(finalPriceInRedisStr);


        // 2. DB에서 모든 입찰 기록을 가져와, 현재 테스트의 경매 기록만 필터링 (BidRepository 수정 없이 검증)
        List<Bid> allBidsInDb = bidRepository.findAll();
        List<Bid> successfulBidsForAuction = allBidsInDb.stream()
                .filter(bid -> bid.getAuction().getId().equals(testAuction.getId()))
                .collect(Collectors.toList());

        BigDecimal maxPriceInDb = successfulBidsForAuction.stream()
                .map(Bid::getBidPrice)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        long successCount = successfulBidsForAuction.size();

        System.out.println("총 요청 수: " + numberOfThreads);
        System.out.println("성공한 입찰 수: " + successCount);
        System.out.println("실패한 입찰 수: " + failureCount.get());
        System.out.println("Redis 최종 입찰가: " + finalPriceInRedis);
        System.out.println("DB 기반 최고 입찰가: " + maxPriceInDb);

        // 3. 최종 검증
        // 3-1. DB에 기록된 최고가와 Redis에 기록된 최고가가 일치하는지 (가장 중요한 검증)
        assertThat(finalPriceInRedis).isEqualByComparingTo(maxPriceInDb);

        // 3-2. 성공한 입찰이 1개 이상 존재해야 함
        assertThat(successCount).isGreaterThan(0);

        // 3-3. 성공한 요청과 실패한 요청의 합이 전체 요청 수와 같은지
        assertThat(numberOfThreads).isEqualTo(successCount + failureCount.get());
    }
}
