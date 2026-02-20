package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.statistics.entity.DailyStatistics;

/**
 * 관리자 통계 조회 API 응답용
 */
@Getter
@AllArgsConstructor
public class AdminStatisticsRes {

    private String date;
    private AuctionTradeStats auctionTrade;
    private CreditStats credit;

    /** 경매/거래 통계 */
    @Getter
    @AllArgsConstructor
    public static class AuctionTradeStats {
        private int auctionTotalCount;       // 전체 경매 수
        private int auctionSuccessCount;     // 낙찰 성공 수
        private int auctionFailCount;        // 유찰 수
        private int auctionBlockedCount;     // 차단된 경매 수
        private double auctionSuccessRate;   // 낙찰률
        private long completedOrderCount;    // 거래 완료 수
        private long canceledOrderCount;     // 거래 취소 수
    }

    /** 크레딧 통계 */
    @Getter
    @AllArgsConstructor
    public static class CreditStats {
        private long totalCreditAmount;     // 총 크레딧 (합계)
        private long chargeAmount;          // 충전 금액
        private long withdrawAmount;        // 환전 금액
        private long depositForfeitAmount;  // 보증금 회수 금액
        private long depositReturnAmount;   // 보증금 반환 금액
        private long settlementAmount;      // 낙찰 정산 금액
    }

    public static AdminStatisticsRes from(DailyStatistics stat) {
        AuctionTradeStats auctionTrade = createAuctionTradeStats(stat);
        CreditStats credit = createCreditStats(stat);
        return new AdminStatisticsRes(stat.getStatDate().toString(), auctionTrade, credit);
    }

    /** 경매/거래 통계 생성 */
    private static AuctionTradeStats createAuctionTradeStats(DailyStatistics stat) {
        int total = stat.getAuctionTotalCount();
        double auctionSuccessRate = (total > 0)
                ? Math.round((double) stat.getAuctionSuccessCount() / total * 1000) / 10.0
                : 0.0;

        return new AuctionTradeStats(
                stat.getAuctionTotalCount(),
                stat.getAuctionSuccessCount(),
                stat.getAuctionFailCount(),
                stat.getAuctionBlockedCount(),
                auctionSuccessRate,
                stat.getCompletedOrderCount(),
                stat.getCanceledOrderCount()
        );
    }

    /** 크레딧 통계 생성 */
    private static CreditStats createCreditStats(DailyStatistics stat) {
        long totalCreditAmount = stat.getWithdrawAmount()
                .add(stat.getDepositForfeitAmount())
                .add(stat.getDepositReturnAmount())
                .add(stat.getSettlementAmount())
                .longValue();

        return new CreditStats(
                totalCreditAmount,
                stat.getChargeAmount().longValue(),
                stat.getWithdrawAmount().longValue(),
                stat.getDepositForfeitAmount().longValue(),
                stat.getDepositReturnAmount().longValue(),
                stat.getSettlementAmount().longValue()
        );
    }
}
