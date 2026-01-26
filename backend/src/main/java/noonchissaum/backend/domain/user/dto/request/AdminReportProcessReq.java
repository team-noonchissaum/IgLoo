package noonchissaum.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 신고 처리 요청
 */

@Getter
@NoArgsConstructor
public class AdminReportProcessReq {

    @NotBlank(message = "처리 상태는 필수입니다.")
    private String status;

    private String adminNote;
}
