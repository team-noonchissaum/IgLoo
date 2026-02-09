package noonchissaum.backend.domain.report.repository;

import noonchissaum.backend.domain.report.entity.Report;
import noonchissaum.backend.domain.report.entity.ReportStatus;
import noonchissaum.backend.domain.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    //신고 목록 조회
    @Query("SELECT r FROM Report r JOIN FETCH r.reporter")
    Page<Report> findAllWithReporter(Pageable pageable);

    //신고 상세 조회
    @Query("SELECT r FROM Report r JOIN FETCH r.reporter WHERE r.id = :reportId")
    Optional<Report> findByIdWithReporter(@Param("reportId") Long reportId);

    //신고 상태별 신고 목록 조회(PENDING, PROCESSED, REJECTED)
    @Query("SELECT r FROM Report r JOIN FETCH r.reporter WHERE r.status = :status")
    Page<Report> findByStatusWithReporter(@Param("status") ReportStatus status, Pageable pageable);

    // 신고 상태 + 대상 타입별 조회 (신고 관리: 경매 신고만, 대기 중만)
    @Query("SELECT r FROM Report r JOIN FETCH r.reporter WHERE r.status = :status AND r.targetType = :targetType")
    Page<Report> findByStatusAndTargetTypeWithReporter(
            @Param("status") ReportStatus status,
            @Param("targetType") ReportTargetType targetType,
            Pageable pageable);

    //reporterId로 직접조회
    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    // 유저별 신고 당한 횟수 조회
    long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);

    // 가장 최근 신고 조회
    Optional<Report> findTopByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            ReportTargetType targetType,
            Long targetId
    );

    // 신고 수 카운트 (상태별)
    long countByTargetTypeAndTargetIdAndStatus(
            ReportTargetType targetType,
            Long targetId,
            ReportStatus status
    );

    // 신고 상태 일괄 업데이트
    @Modifying
    @Query("UPDATE Report r SET r.status = :newStatus, r.processedAt = CURRENT_TIMESTAMP " +
            "WHERE r.targetType = :targetType AND r.targetId = :targetId AND r.status = :oldStatus")
    void updateStatusByTargetTypeAndTargetIdAndStatus(
            @Param("targetType") ReportTargetType targetType,
            @Param("targetId") Long targetId,
            @Param("oldStatus") ReportStatus oldStatus,
            @Param("newStatus") ReportStatus newStatus
    );

    // 특정 대상에 대한 신고 목록 조회
    @Query("SELECT r FROM Report r JOIN FETCH r.reporter WHERE r.targetType = :targetType AND r.targetId = :targetId ORDER BY r.createdAt DESC")
    List<Report> findByTargetTypeAndTargetId(
            @Param("targetType") ReportTargetType targetType,
            @Param("targetId") Long targetId
    );
}
