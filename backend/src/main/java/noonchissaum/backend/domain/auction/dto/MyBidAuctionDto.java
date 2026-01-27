package noonchissaum.backend.domain.auction.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class MyBidAuctionDto{

    public record MyBidAuctionDtoReq(
            Long auctionId,
            String title,
            Long currentPrice,
            Long myLastBidPrice,
            AuctionStatus status,
            LocalDateTime endAt
    ) {}
}
