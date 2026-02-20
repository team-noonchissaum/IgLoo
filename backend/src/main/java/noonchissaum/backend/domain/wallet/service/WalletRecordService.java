package noonchissaum.backend.domain.wallet.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletRecordService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRecordService walletTransactionRecordService;
    private final StringRedisTemplate redisTemplate;
    private final WalletService walletService;

    @Transactional
    public void saveWalletRecord(Long userId, BigDecimal bidAmount, Long previousBidderId , BigDecimal refundAmount,Long auctionId){
        Wallet newBidUserWallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));

        newBidUserWallet.bid(bidAmount);
        walletTransactionRecordService.record(newBidUserWallet, TransactionType.BID_HOLD,bidAmount,auctionId);

        if (previousBidderId != null && previousBidderId != -1L) {
            Wallet prevBidUserWallet = walletRepository.findByUserId(previousBidderId)
                    .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
            prevBidUserWallet.bidCanceled(refundAmount);
            walletTransactionRecordService.record(prevBidUserWallet, TransactionType.BID_RELEASE,refundAmount,auctionId);
        }

    }

    /**
     * 경매 차단 확정시 환불 로직
     */
    @Transactional
    public void refundBlockedAuctionBid(Long userId, BigDecimal refundAmount, Long auctionId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
        wallet.bidCanceled(refundAmount);
        walletTransactionRecordService.record(wallet, TransactionType.BID_RELEASE, refundAmount, auctionId);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            //방어적 코드
            walletService.getBalance(userId);
            redisTemplate.opsForValue().increment(RedisKeys.userBalance(userId), refundAmount.longValue());
            redisTemplate.opsForValue().increment(RedisKeys.userLockedBalance(userId), refundAmount.negate().longValue());
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                walletService.getBalance(userId);
                redisTemplate.opsForValue().increment(RedisKeys.userBalance(userId), refundAmount.longValue());
                redisTemplate.opsForValue().increment(RedisKeys.userLockedBalance(userId), refundAmount.negate().longValue());
            }
        });
    }

    /**
     * 입찰 롤백 시 DB 지갑 역처리
     * - 차단 유저: bidCanceled (환불)
     * - 이전 입찰자: bid (재동결)
     */
    @Transactional
    public void rollbackWalletRecord(Long blockedUserId, Long previousBidderId,
                                     BigDecimal blockedUserRefundAmount, BigDecimal previousBidderRelockAmount,
                                     Long auctionId) {
        Wallet blockedUserWallet = walletRepository.findByUserId(blockedUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
        blockedUserWallet.bidCanceled(blockedUserRefundAmount);
        walletTransactionRecordService.record(blockedUserWallet, TransactionType.BID_RELEASE, blockedUserRefundAmount, auctionId);

        if (previousBidderId != null && previousBidderId != -1L) {
            Wallet prevBidUserWallet = walletRepository.findByUserId(previousBidderId)
                    .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
            prevBidUserWallet.bid(previousBidderRelockAmount);
            walletTransactionRecordService.record(prevBidUserWallet, TransactionType.BID_HOLD, previousBidderRelockAmount, auctionId);
        }
    }
}
