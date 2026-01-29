package noonchissaum.backend.domain.task.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.service.AuctionRecordService;
import noonchissaum.backend.domain.auction.service.BidRecordService;
import noonchissaum.backend.domain.auction.service.BidService;
import noonchissaum.backend.domain.wallet.service.WalletRecordService;
import noonchissaum.backend.domain.wallet.service.WalletTransactionRecordService;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.domain.task.dto.DbUpdateEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class DbEventListener {

    private final WalletRecordService walletRecordService;
    private final BidRecordService bidRecordService;
    private final BidService bidService;
    private final AuctionRecordService auctionRecordService;
    private final StringRedisTemplate redisTemplate;
    private final AsyncTaskTxService asyncTaskTxService;

    @Async("DBTaskExcutor")
    @EventListener
    @Transactional
    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void handleWalletUpdate(DbUpdateEvent event) {
        log.info("비동기 DB 업데이트 시작 - 유저: {}", event.userId());

        asyncTaskTxService.startTask(event);

        //bid 저장
        if (!bidService.isExistRequestId(event.requestId())) {
            bidRecordService.saveBidRecord(event.auctionId(), event.userId(), event.bidAmount(), event.requestId());
        }

        //wallet 저장
        walletRecordService.saveWalletRecord(event.userId(),event.bidAmount(),event.previousBidderId(),event.refundAmount(),event.auctionId());

        //추후 auction 저장 로직 추가
        auctionRecordService.saveAuction(event.auctionId(), event.userId(),event.bidAmount());

        // 작업이 완료되었는지 db저장
        registerAfterCommit(event);



    }

    private void registerAfterCommit(DbUpdateEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 원래라면 @Transactional 안에서는 active여야 함. 방어 코드.
            log.warn("Transaction synchronization is not active. Fallback to immediate execution.");
            markSuccessAndCleanupRedis(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                markSuccessAndCleanupRedis(event);
            }
        });
    }

    private void markSuccessAndCleanupRedis(DbUpdateEvent event) {
        // (A) 성공 마킹은 REQUIRES_NEW로 안전하게 확정
        asyncTaskTxService.markSuccess(event.requestId());

        // (B) Redis pending 제거
        String userKey = RedisKeys.pendingUser(event.userId());
        redisTemplate.opsForSet().remove(userKey, event.requestId());

        // previousBidderId가 -1이면 건드리지 않기 권장
        if (event.previousBidderId() != null && event.previousBidderId() != -1L) {
            String prevUserKey = RedisKeys.pendingUser(event.previousBidderId());
            redisTemplate.opsForSet().remove(prevUserKey, event.requestId());
        }
    }

    // 재시도 3번 후에도 안됬을 경우
    @Recover
    public void recover(Exception e, DbUpdateEvent event) {
        log.error("최종 DB 업데이트 실패! 직접 확인 필요 - 유저ID: {}, 금액: {}",
                event.userId(), event.bidAmount(), e);
    }
}
