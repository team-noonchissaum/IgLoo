package noonchissaum.backend.domain.wallet.dto.req;

import java.math.BigDecimal;

public record WithdrawalReq(
        BigDecimal amount,
        String bankName,
        String accountNumber
) {}
