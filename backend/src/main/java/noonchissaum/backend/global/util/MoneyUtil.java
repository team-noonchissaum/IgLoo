package noonchissaum.backend.global.util;

public final class MoneyUtil {
    private MoneyUtil() {}

    /**
    * startPrice 기준 보증금
    */
    public static int calcDeposit(int startPrice){
        if (startPrice <= 10000){
            return (int) Math.ceil(startPrice * 0.10);
        }
        return 1000;
    }

    /**
     * 최소 입찰가: 현재가의 10%증가 + 10원 단위 올림
     */
    public static int nextMinBidPrice(int currentPrice) {
        int increased = (int) Math.ceil(currentPrice * 0.10);
        int nextPrice = currentPrice + increased;
        return ceilTo10(nextPrice);
    }

    private static int ceilTo10(int value) {
        return (int) (Math.ceil(value / 10.0) * 10);
    }

}
