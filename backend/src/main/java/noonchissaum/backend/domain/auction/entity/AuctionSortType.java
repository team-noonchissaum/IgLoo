package noonchissaum.backend.domain.auction.entity;

import org.springframework.data.domain.Sort;

public enum AuctionSortType {
    LATEST,        // 최신순 (startAt DESC)
    BID_COUNT,     // 입찰 횟수순 (bidCount DESC)
    DEADLINE,      // 마감 임박순 (endAt ASC)
    PRICE_HIGH,    // Redis ZSET (DESC)
    PRICE_LOW;     // Redis ZSET (ASC)

    public boolean isRedisPriceSort() {
        return this == PRICE_HIGH || this == PRICE_LOW;
    }

    public Sort toSort() {
        return switch (this) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "startAt");
            case BID_COUNT -> Sort.by(Sort.Direction.DESC, "bidCount");
            case DEADLINE -> Sort.by(Sort.Direction.ASC, "endAt");
            // price는 db정렬 x
            case PRICE_HIGH, PRICE_LOW -> Sort.unsorted();
        };
    }
}
