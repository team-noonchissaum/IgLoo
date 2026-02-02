package noonchissaum.backend.domain.auction.dto.ws;


import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder

public class AuctionEndedPayload {
    private Long auctionId;
    private Long winnerUserId;
    private Long finalPrice;
    private Integer bidCount;
    private LocalDateTime endedAt;
    private String message;
}
