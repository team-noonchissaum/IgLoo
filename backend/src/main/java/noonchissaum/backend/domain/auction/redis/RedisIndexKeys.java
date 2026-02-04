package noonchissaum.backend.domain.auction.redis;

public class RedisIndexKeys {
    private RedisIndexKeys() {}

    public static final String PRICE_LIVE_ZSET = "auction:idx:price:live"; // ZSET

    // 카테고리별 live 가격 인덱스
    public static String priceLiveByCategory(Long categoryId) {
        return "auction:idx:price:live:cat:" + categoryId; // ZSET
    }

}
