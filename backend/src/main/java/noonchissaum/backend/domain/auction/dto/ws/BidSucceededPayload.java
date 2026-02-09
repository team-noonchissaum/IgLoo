package noonchissaum.backend.domain.auction.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidSucceededPayload {

    private Long auctionId;
    private Long currentPrice;
    private Long currentBidderId;
    private Integer bidCount;
    private LocalDateTime endAt;
    private Boolean isExtended;
}
