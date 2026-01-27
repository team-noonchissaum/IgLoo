package noonchissaum.backend.global.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.service.BidRecordService;
import noonchissaum.backend.domain.auction.service.BidService;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.domain.wallet.service.WalletRecordService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletEventListener {

    private final WalletRepository walletRepository;
    private final WalletRecordService walletRecordService;
    private final BidRecordService bidRecordService;
    private final BidService bidService;
    private final RedisTemplate<Object, Object> redisTemplate;

    @Async("walletTaskExcutor")
    @EventListener
    @Transactional
    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void handleWalletUpdate(DbUpdateEvent event) {
        log.info("비동기 DB 업데이트 시작 - 유저: {}", event.userId());

        //bid 저장
        if (!bidService.isExistRequestId(event.requestId())) {
            bidRecordService.saveBidRecord(event.auctionId(), event.userId(), event.bidAmount(), event.requestId());
        }

        //wallet 저장
        walletRecordService.saveWalletRecord(event.userId(),event.bidAmount(),event.previousBidderId(),event.refundAmount());

        //추후 auction 저장 로직 추가

    }

    // 재시도 3번 후에도 안됬을 경우
    @Recover
    public void recover(Exception e, DbUpdateEvent event) {
        log.error("최종 DB 업데이트 실패! 직접 확인 필요 - 유저ID: {}, 금액: {}",
                event.userId(), event.bidAmount(), e);
    }
}
