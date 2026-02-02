package noonchissaum.backend.domain.wallet.dto.withdrawal.req;

import java.math.BigDecimal;

public record WithdrawalReq(
        BigDecimal amount,
        String bankName,
        String accountNumber
) {}
