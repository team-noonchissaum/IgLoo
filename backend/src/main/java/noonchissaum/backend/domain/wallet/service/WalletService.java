package noonchissaum.backend.domain.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.wallet.dto.WalletUpdateEvent;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final StringRedisTemplate redisTemplate;
    private final WalletRepository walletRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 입찰 시 지갑 변화 메서드
//    public void processBidWallet(Long userId, Long previousBidderId,
//                                 BigDecimal bidAmount, BigDecimal currentPrice) {
//        Wallet newBidUserWallet = walletRepository.findByUserId(userId)
//                .orElseThrow(() -> new RuntimeException("user not found"));
//        Wallet prevBidUserWallet = walletRepository.findByUserId(previousBidderId)
//                .orElseThrow(() -> new RuntimeException("user not found"));
//
//        prevBidUserWallet.bidCanceled(currentPrice);
//        newBidUserWallet.bid(bidAmount);
//    }

    public void processBidWallet(Long userId, Long previousBidderId, BigDecimal bidAmount, BigDecimal currentPrice) {
        String userKey = "user:" + userId + ":balance";

        // [2단계] Redis 상에서 변화 입력 (즉시 차감)
        // 실제로는 decrement 후 결과값이 0보다 작으면 롤백하는 로직이 추가됩니다.
        Long remain = redisTemplate.opsForValue().decrement(userKey, bidAmount.longValue());
        if (remain < 0) {
            redisTemplate.opsForValue().increment(userKey, bidAmount.longValue());
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 이전 유저는 redis상에서 잔액 추가
        if (previousBidderId != null && previousBidderId != -1L) {
            String prevUserKey = "user:" + previousBidderId + ":balance";
            redisTemplate.opsForValue().increment(prevUserKey, currentPrice.longValue());
        }

        // [3단계] DB에 변화 처리하기 (비동기 이벤트 발행)
        eventPublisher.publishEvent(new WalletUpdateEvent(userId, previousBidderId, bidAmount, currentPrice));
    }

    public void getBalance(Long userId) {
        String userKey = "user:" + userId + ":balance";

        if (Boolean.FALSE.equals(redisTemplate.hasKey(userKey))) {
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
            redisTemplate.opsForValue().set(userKey, wallet.getBalance().toPlainString());
        }
    }
}
