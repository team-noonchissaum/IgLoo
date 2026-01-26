package noonchissaum.backend.domain.user.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReportProcessReq {
    private String status; // APPROVED, REJECTED, PENDING
}
