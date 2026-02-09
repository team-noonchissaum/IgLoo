package noonchissaum.backend.domain.coupon.dto.res;

import noonchissaum.backend.domain.coupon.entity.CouponIssued;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IssuedCouponRes(
        Long issuedCouponId,
        String reason,
        BigDecimal amount,
        LocalDate expiration
) {
    public static IssuedCouponRes from(CouponIssued coupon) {
        return new IssuedCouponRes(
                coupon.getId(),
                coupon.getReason(),
                coupon.getCoupon().getAmount(),
                coupon.getExpiredAt()
        );
    }
}
