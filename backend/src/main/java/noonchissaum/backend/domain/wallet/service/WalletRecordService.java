package noonchissaum.backend.domain.wallet.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletRecordService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRecordService walletTransactionRecordService;

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

}
