package noonchissaum.backend.domain.auction.dto.ws;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Builder
public class BidStartPayload {
    private Long userId;
    private Long auctionId;
    private Long bidPrice;
    private LocalDateTime createdAt;
}
