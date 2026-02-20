package noonchissaum.backend.domain.statistics.repository;

import noonchissaum.backend.domain.statistics.entity.DailyStatistics;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface DailyStatisticsRepository extends JpaRepository<DailyStatistics, Long> {

    /** 특정 날짜 통계 조회 */
    Optional<DailyStatistics> findByStatDate(LocalDate statDate);

    /** 특정 날짜 통계 존재 여부 (중복 방지용) */
    boolean existsByStatDate(LocalDate statDate);

    // 등록된 경매 수
    @Query("SELECT COUNT(a) FROM Auction a WHERE a.createdAt >= :start AND a.createdAt < :end")
    long countAuctionsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 낙찰 성공 수
    @Query("SELECT COUNT(a) FROM Auction a WHERE a.status = 'ENDED' AND a.endAt >= :start AND a.endAt < :end")
    long countSuccessAuctionsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 유찰 수
    @Query("SELECT COUNT(a) FROM Auction a WHERE a.status = 'FAILED' AND a.endAt >= :start AND a.endAt < :end")
    long countFailedAuctionsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 차단된 경매 수
    @Query("SELECT COUNT(a) FROM Auction a WHERE a.status = 'BLOCKED' AND a.createdAt >= :start AND a.createdAt < :end")
    long countBlockedAuctionsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 거래 완료 수
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'COMPLETED' AND o.createdAt >= :start AND o.createdAt < :end")
    long countCompletedOrdersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 거래 취소 수
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'CANCELED' AND o.createdAt >= :start AND o.createdAt < :end")
    long countCanceledOrdersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 크레딧 통계
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t WHERE t.type = :type AND t.createdAt >= :start AND t.createdAt < :end")
    BigDecimal sumAmountByTypeBetween(@Param("type") TransactionType type, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
