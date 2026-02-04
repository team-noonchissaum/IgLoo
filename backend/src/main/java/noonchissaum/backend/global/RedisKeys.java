package noonchissaum.backend.global;

public final class RedisKeys {

    private RedisKeys() {}

    /**
     * user
     */
    public static String userLock(Long userId) {
        return "lock:user:" + userId;
    }
    public static String userBalance(Long userId) {
        return "user:" + userId + ":balance";
    }

    public static String userLockedBalance(Long userId) {
        return "user:" + userId + ":lockedBalance";
    }

    public static String pendingUser(Long userId) {
        return "pending:user:" + userId;
    }

    /**
     * auction
     */
    public static String auctionLock(Long auctionId) {
        return "lock:auction:" + auctionId;
    }

    public static String auctionCurrentPrice(Long auctionId) {
        return "auction:" + auctionId + ":currentPrice";
    }

    public static String auctionCurrentBidder(Long auctionId) {
        return "auction:" + auctionId + ":currentBidder";
    }

    public static String auctionCurrentBidCount(Long auctionId) {
        return "auction:" + auctionId + ":currentBidCount";
    }

    public static String auctionEndTime(Long auctionId) {
        return "auction:" + auctionId + ":endTime";
    }

    public static String auctionImminentMinutes(Long auctionId) { return "auction:" + auctionId + ":imminentMinutes"; }

    public static String auctionIsExtended(Long auctionId) { return "auction:" + auctionId + ":isExtended"; }

    /**
     * Bid Idempotency / Pending
     */
    public static String bidIdempotency(String requestId) {
        return "bid_idempotency:" + requestId;
    }

    public static String pendingBidInfo(String requestId) {
        return "pending_bid_info:" + requestId;
    }

    public static String pendingBidRequestsSet() {
        return "pending_bid_requests";
    }
}
