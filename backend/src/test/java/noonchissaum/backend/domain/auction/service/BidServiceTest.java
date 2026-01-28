package noonchissaum.backend.domain.auction.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "file:C:/exam/finalprojects/IgLoo/.env.dev")
class BidServiceTest {

    @Autowired
    private BidService bidService;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("100명이 동시에 입찰하면 1명만 성공")
    void concurrencyTest() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        Long auctionId = 1L;
        BigDecimal bidAmount = new BigDecimal("10000");

        for (int i = 0; i < threadCount; i++) {
            long userId = i; // 각기 다른 유저 ID
            String bidUuid = UUID.randomUUID().toString();
            executorService.submit(() -> {
                try {
                    bidService.placeBid(auctionId, userId, bidAmount, bidUuid);
                } catch (Exception e) {
                    System.out.println("입찰 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 끝날 때까지 대기

        // 결과 검증: Redis에 저장된 입찰 횟수가 1인지 확인
        String count = redisTemplate.opsForValue().get("auction:" + auctionId + ":currentBidCount");
        assertThat(count).isEqualTo("1");
    }
}