package noonchissaum.backend.domain.wallet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.wallet.dto.WalletUpdateEvent;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.context.event.EventListener;
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

    @Async("walletTaskExcutor")
    @EventListener
    @Transactional
    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void handleWalletUpdate(WalletUpdateEvent event) {
        log.info("비동기 DB 업데이트 시작 - 유저: {}", event.userId());

        Wallet newBidUserWallet = walletRepository.findByUserId(event.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));

        newBidUserWallet.bid(event.bidAmount());

        if (event.previousBidderId() == null || event.previousBidderId() == -1L) {
            return;
        }

        Wallet prevBidUserWallet = walletRepository.findByUserId(event.previousBidderId())
                .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));

        prevBidUserWallet.bidCanceled(event.refundAmount());
    }

    // 재시도 3번 후에도 안됬을 경우
    @Recover
    public void recover(Exception e, WalletUpdateEvent event) {
        log.error("최종 DB 업데이트 실패! 직접 확인 필요 - 유저ID: {}, 금액: {}",
                event.userId(), event.bidAmount(), e);
    }
}
