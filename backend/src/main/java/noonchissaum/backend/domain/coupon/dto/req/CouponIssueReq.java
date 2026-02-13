package noonchissaum.backend.domain.coupon.dto.req;

import java.util.List;

public record CouponIssueReq(
        Long couponId,
        List<Long> userIds,
        String reason
) {
}
