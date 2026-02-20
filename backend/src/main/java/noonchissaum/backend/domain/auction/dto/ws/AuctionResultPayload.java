package noonchissaum.backend.domain.auction.dto.ws;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuctionResultPayload {
    private Long auctionId;
    private String result;        // "SUCCESS" or "FAILED"
    private Long winnerUserId;    // 유찰이면 null
    private Long finalPrice;      // 유찰이면 null 또는 0
    private Integer bidCount;
    private LocalDateTime decidedAt;
}
