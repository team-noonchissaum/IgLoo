package noonchissaum.backend.domain.auction.dto;

import noonchissaum.backend.domain.auction.entity.AuctionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MyBidAuctionRes(
        Long auctionId,
        String itemTitle,
        Long myHighestBidPrice,
        Long currentPrice,
        Boolean isHighestBidder,
        AuctionStatus auctionStatus,
        LocalDateTime endTime,
        Integer bidCount
) {
    public static MyBidAuctionRes of(
            Long auctionId,
            String itemTitle,
            Long myHighestBidPrice,
            Long currentPrice,
            boolean isHighestBidder,
            AuctionStatus auctionStatus,
            LocalDateTime endTime,
            int bidCount
    ) {
        return new MyBidAuctionRes(
                auctionId,
                itemTitle,
                myHighestBidPrice == null ? 0L : myHighestBidPrice,
                currentPrice == null ? 0L : currentPrice,
                isHighestBidder,
                auctionStatus,
                endTime,
                bidCount
        );
    }
}