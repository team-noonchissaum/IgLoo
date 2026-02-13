package noonchissaum.backend.domain.coupon.dto.req;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.UniqueElements;

import java.math.BigDecimal;

public record CouponDefReq(
        @NotNull @UniqueElements
        String name,
        BigDecimal amount
) {
}
