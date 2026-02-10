package noonchissaum.backend.domain.coupon.dto.res;

import java.math.BigDecimal;

public record CouponRes(
        Long couponId,
        String name,
        BigDecimal amount
) {
}
