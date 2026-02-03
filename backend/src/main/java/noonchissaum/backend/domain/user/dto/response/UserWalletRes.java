package noonchissaum.backend.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
public class UserWalletRes {
    private BigDecimal balance;           // 사용 가능 크레딧
    private BigDecimal lockedBalance;     // 잠긴 크레딧 (입찰 중)
    private BigDecimal totalBalance;      // 전체 크레딧
}