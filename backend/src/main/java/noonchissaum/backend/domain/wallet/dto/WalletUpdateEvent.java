package noonchissaum.backend.domain.wallet.dto;

import java.math.BigDecimal;

public record WalletUpdateEvent(
        Long userId,              // 신규 입찰자 ID
        Long previousBidderId,    // 유저 ID (첫 입찰이면 -1L 또는 null)
        BigDecimal bidAmount,     // 신규 입찰 금액 (차감 및 동결할 금액)
        BigDecimal refundAmount,   // 이전 입찰자에게 돌려줄 금액
        Long auctionId,         //bid 저장용 auctionId
        String requestId        //bid 저장용 requestId
) {
}
