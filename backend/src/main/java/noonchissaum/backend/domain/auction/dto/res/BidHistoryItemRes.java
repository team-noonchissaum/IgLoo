package noonchissaum.backend.domain.auction.dto.res;

import noonchissaum.backend.domain.auction.entity.Bid;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BidHistoryItemRes(
        Long bidId,
        String bidderNickname,
        BigDecimal bidPrice,
        LocalDateTime createdAt
) {
    public static BidHistoryItemRes from(Bid bid) {
        return new BidHistoryItemRes(
                bid.getId(),
                bid.getBidder().getNickname(),
                bid.getBidPrice(),
                bid.getCreatedAt()
        );
    }
}
