package noonchissaum.backend.domain.auction.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuctionSnapshotPayload {
    private Long auctionId;

    private Long currentPrice;
    private Long currentBidderId;
    private Integer bidCount;

    private LocalDateTime endAt;
    private Integer imminentMinutes;
    private Boolean isExtended;
}
