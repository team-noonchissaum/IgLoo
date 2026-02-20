package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class MyPageRes {

    private Long userId;
    private String email;
    private String nickname;
    private String profileUrl;
    private BigDecimal balance;

}
