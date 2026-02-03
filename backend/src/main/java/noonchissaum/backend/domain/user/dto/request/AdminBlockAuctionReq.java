package noonchissaum.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminBlockAuctionReq {

    @NotBlank(message = "차단 사유를 입력해 주세요.")
    private String reason;
}
