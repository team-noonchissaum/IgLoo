package noonchissaum.backend.domain.user.repository;

import noonchissaum.backend.domain.report.entity.Report;
import noonchissaum.backend.domain.report.entity.ReportStatus;
import noonchissaum.backend.domain.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByStatus(ReportStatus status);

    //신고 목록 조회
    @Query("SELECT r FROM Report r JOIN FETCH r.reporter")
    Page<Report> findAllWithReporter(Pageable pageable);

    //신고 상세 조회
    @Query("SELECT r FROM Report r JOIN FETCH r.reporter WHERE r.id = :reportId")
    Optional<Report> findByIdWithReporter(@Param("reportId") Long reportId);

    //신고 상태별 신고 목록 조회(PENDING,RESOLVED, REJECTED)
    @Query("SELECT r FROM Report r JOIN FETCH r.reporter WHERE r.status = :status")
    Page<Report> findByStatusWithReporter(@Param("status") ReportStatus status, Pageable pageable);

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

}
