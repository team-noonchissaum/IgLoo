package noonchissaum.backend.global.util;

public final class MoneyUtil {
    private MoneyUtil() {}

    /**
    * startPrice 기준 보증금
    */
    private static final int FIXED_MIN_DEPOSIT = 1_000;
    private static final double DEPOSIT_RATE = 0.10;

    public static int calcDeposit(int startPrice){
        int rateAmount = (int) Math.ceil(startPrice * DEPOSIT_RATE);
        return Math.max(FIXED_MIN_DEPOSIT, rateAmount);
    }
    /**
     * 경매 시작가가 0원이면 첫입찰은 0원
     * 다음 입찰은 1000원부터 시작하고 10%씩 증가
     * 시작금액이 0이 아니라면 10%씩 증가함
     */
    public static int getMinBidPrice(int currentPrice, int bidCount) {
        if (bidCount == 0) return 0;
        if (bidCount == 1) return 1000;

        // 현재가에 1.1배를 곱한 뒤 10원 단위로 올림
        return (int) (Math.ceil((currentPrice * 1.1) / 10.0) * 10);
    }


}
