package noonchissaum.backend.domain.coupon.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.coupon.dto.req.CouponIssueReq;
import noonchissaum.backend.domain.coupon.dto.res.IssuedCouponRes;
import noonchissaum.backend.domain.coupon.service.CouponIssueService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons/issues")
@RequiredArgsConstructor
public class CouponIssueController {

    private final CouponIssueService couponIssueService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<IssuedCouponRes>>> getIssuedCoupons(
            @AuthenticationPrincipal UserPrincipal principal){
        List<IssuedCouponRes> issuedCoupons = couponIssueService.getIssuedCouponsByUserId(principal.getUserId());
        return ResponseEntity.ok(new ApiResponse<>("issuedCoupons", issuedCoupons));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<IssuedCouponRes>> createIssuedCoupon(@RequestBody CouponIssueReq req) {
        couponIssueService.couponIssue(req);
        return ResponseEntity.ok(new ApiResponse<>("issued", null));
    }

    @PostMapping("/{issuedCouponId}")
    public ResponseEntity<ApiResponse<Void>> useIssuedCoupon(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("issuedCouponId") Long issuedCouponId) {
        couponIssueService.useCoupon(principal.getUserId(), issuedCouponId);
        return ResponseEntity.ok(new ApiResponse<>("used", null));
    }
}
