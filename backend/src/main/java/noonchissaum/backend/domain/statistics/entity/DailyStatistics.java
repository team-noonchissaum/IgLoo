package noonchissaum.backend.domain.statistics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일일 통계
 * 매일 자정 배치가 전일 데이터를 집계해서 이 테이블에 저장
 */
@Entity
@Table(name = "daily_statistics", uniqueConstraints = @UniqueConstraint(columnNames = "stat_date"))
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class DailyStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 통계 기준 날짜 (중복 저장 방지를 위해 unique) */
    @Column(name = "stat_date", nullable = false, unique = true)
    private LocalDate statDate;

    /* ===== 거래 통계 ===== */

    /** 거래 완료 수 (구매확정) */
    @Column(name = "completed_order_count", nullable = false)
    private Long completedOrderCount;

    /** 거래 취소 수 */
    @Column(name = "canceled_order_count", nullable = false)
    private Long canceledOrderCount;

    /* ===== 경매 통계 ===== */

    /** 전체 경매 수 */
    @Column(name = "auction_count", nullable = false)
    private int auctionTotalCount;

    /** 낙찰 성공 수 */
    @Column(name = "success_count", nullable = false)
    private int auctionSuccessCount;

    /** 유찰 수 */
    @Column(name = "failed_count", nullable = false)
    private int auctionFailCount;

    /** 차단된 경매 수 */
    @Column(name = "blocked_count", nullable = false)
    private int auctionBlockedCount;

    /* ===== 크레딧 통계 ===== */

    /** 충전 금액 */
    @Column(name = "charge_amount", nullable = false)
    private BigDecimal chargeAmount;

    /** 환전(출금) 금액 */
    @Column(name = "withdraw_amount", nullable = false)
    private BigDecimal withdrawAmount;

    /** 보증금 회수 금액*/
    @Column(name = "deposit_forfeit_amount", nullable = false)
    private BigDecimal depositForfeitAmount;

    /** 보증금 반환 금액 */
    @Column(name = "deposit_return_amount", nullable = false)
    private BigDecimal depositReturnAmount;

    /** 낙찰 정산 금액 */
    @Column(name = "settlement_amount", nullable = false)
    private BigDecimal settlementAmount;

    /** 통계 생성 시각 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
