package noonchissaum.backend.domain.user.repository;

import noonchissaum.backend.domain.report.entity.Report;
import noonchissaum.backend.domain.report.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByStatus(ReportStatus status);
}
