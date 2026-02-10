package noonchissaum.backend.domain.coupon.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.coupon.repository.CouponIssuedRepository;
import noonchissaum.backend.domain.coupon.service.CouponIssueService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CouponExpiredScheduler {
    private final CouponIssueService couponIssueService;
    private final CouponIssuedRepository couponIssuedRepository;

    /**
     * 만료시간이 지난 쿠폰을 만료 상태로 전환합니다.
     * */
    @Scheduled(cron = "0 1 0 * * *")
    public void deleteExpiredCoupons() {
        couponIssueService.expireCoupons();
    }
}
