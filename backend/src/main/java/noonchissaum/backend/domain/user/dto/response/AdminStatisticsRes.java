package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 일일 통계 응답
 */

@Getter
@AllArgsConstructor
public class AdminStatisticsRes {

    private String date;
    private TransactionStats transaction;
    private AuctionStats auction;
    private CreditStats credit;

    @Getter
    @AllArgsConstructor
    public static class TransactionStats {
        private int totalCount;
        private int completedCount;
        private int canceledCount;
    }

    @Getter
    @AllArgsConstructor
    public static class AuctionStats {
        private int totalCount;
        private int successCount;
        private int failedCount;
        private double successRate;
    }

    @Getter
    @AllArgsConstructor
    public static class CreditStats {
        private long totalCharged;
        private long totalUsed;
        private long totalWithdrawn;
    }
}
