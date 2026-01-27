package noonchissaum.backend.domain.wallet.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.repository.WalletRepository;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletRecordService {
    private final WalletRepository walletRepository;


    public void saveWalletRecord(Long userId, BigDecimal bidAmount, Long previousBidderId , BigDecimal refundAmount){
        Wallet newBidUserWallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));

        newBidUserWallet.bid(bidAmount);

        if (previousBidderId != null && previousBidderId != -1L) {
            Wallet prevBidUserWallet = walletRepository.findByUserId(previousBidderId)
                    .orElseThrow(() -> new ApiException(ErrorCode.CANNOT_FIND_WALLET));
            prevBidUserWallet.bidCanceled(refundAmount);
        }

    }

}
