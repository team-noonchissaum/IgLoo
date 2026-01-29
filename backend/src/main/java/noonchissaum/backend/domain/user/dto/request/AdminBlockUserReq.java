package noonchissaum.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 차단 요청
 */

@Getter
@NoArgsConstructor
public class AdminBlockUserReq {

    @NotBlank(message = "차단 사유를 입력해 주세요.")
    private String reason;
}
