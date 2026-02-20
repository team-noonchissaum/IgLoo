package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import noonchissaum.backend.domain.user.enums.DeleteAction;
import java.math.BigDecimal;

@AllArgsConstructor
@Getter
public class UserDeleteAttemptRes {

    private DeleteAction action;
    private BigDecimal balance;
    private String message;

    /**크레딧 없음*/
    public static UserDeleteAttemptRes noBalance() {
        return new UserDeleteAttemptRes(
                DeleteAction.CONFIRM_REQUIRED,
                BigDecimal.ZERO,
                "정말 탈퇴하시겠습니까?"
        );
    }
    /**첫 시도-환전 권장 알림*/
    public static UserDeleteAttemptRes firstAttempt(BigDecimal balance) {
        return new UserDeleteAttemptRes(
                DeleteAction.WARN_FIRST,
                balance,
                "크레딧 " + balance + "원이 남아있습니다. 환전 후 다시 시도해주세요."
        );
    }

    /**두번째 이상 시도-환전 포기 확인*/
    public static UserDeleteAttemptRes confirmRequired(BigDecimal balance) {
        return new UserDeleteAttemptRes(
                DeleteAction.CONFIRM_REQUIRED,
                balance,
                "크레딧 " + balance + "원을 포기하고 탈퇴하시겠습니까?"
        );
    }
}
