package noonchissaum.backend.domain.auction.dto.req;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlaceBidReq(
        @NotNull
        Long auctionId,
        @NotNull
        BigDecimal bidAmount,
        @NotNull
        String requestId
)
{
}
