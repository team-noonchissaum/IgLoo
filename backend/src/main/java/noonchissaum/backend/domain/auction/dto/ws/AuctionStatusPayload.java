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
public class AuctionStatusPayload {
    private Long auctionId;

    private Boolean hasWinner;
    private Long WinnerUserId;
    private Long finalPrice;

    private LocalDateTime endAt;
    private Boolean isExtended;
}
