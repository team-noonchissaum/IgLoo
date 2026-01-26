package noonchissaum.backend.domain.auction.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlaceBidReq(
        @NotNull
        Long auctionId,
        @NotNull
        Long userId,
        @NotNull
        BigDecimal bidAmount,
        @NotNull
        String requestId
)
{
}
