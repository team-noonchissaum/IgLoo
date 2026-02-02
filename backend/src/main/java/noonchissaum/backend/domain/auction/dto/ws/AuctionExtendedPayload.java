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
public class AuctionExtendedPayload {
    private Long auctionId;
    private LocalDateTime endAt;
    private boolean isExtended;
    private Integer extendedMinutes;
}
