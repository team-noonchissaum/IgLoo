package noonchissaum.backend.domain.report.entity;

import jakarta.persistence.*;
import lombok.*;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.entity.BaseTimeEntity;
import org.hibernate.annotations.Formula;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    //신고자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private ReportTargetType targetType;

    //신고 대상 유저
    @Column(name = "target_id")
    private Long targetId;

    //신고 이유
    @Column(length = 255)
    private String reason;

    //상세사유
    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReportStatus status;

    //관리자 처리 결과
    @Column(name = "process_result", length = 500)
    private String processResult;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    //reporterId읽기전용
    @Formula("reporter_id")
    @Column(name = "reporter_id", insertable = false, updatable = false)
    private Long reporterId;

    /**
     * 관리자 계정용
     */
    public void process(ReportStatus status) {
        if (this.status != ReportStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 신고입니다.");
        }
        this.status = status;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 처리 결과 기록
     */
    public void addProcessResult(String processResult) {
        this.processResult = processResult;
    }

    /**
     * 신고 생성
     */
    public static Report create(
            User reporter,
            ReportTargetType targetType,
            Long targetId,
            String reason,
            String description
    ) {
        Report report = new Report();
        report.reporter = reporter;
        report.targetType = targetType;
        report.targetId = targetId;
        report.reason = reason;
        report.description = description;
        report.status = ReportStatus.PENDING;
        return report;
    }
}
