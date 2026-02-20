package noonchissaum.backend.domain.coupon.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.coupon.dto.req.CouponDefReq;
import noonchissaum.backend.domain.coupon.dto.res.CouponRes;
import noonchissaum.backend.domain.coupon.service.CouponService;
import noonchissaum.backend.global.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons")
public class CouponDefController {

    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CouponRes>>> getAllCoupons() {
        List<CouponRes> allCoupons = couponService.getAllCoupons();
        return ResponseEntity.ok(new ApiResponse<>("coupons", allCoupons));
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<ApiResponse<CouponRes>> getCoupon(@PathVariable("couponId") Long couponId) {
        CouponRes coupon = couponService.getCouponById(couponId);
        return ResponseEntity.ok(new ApiResponse<>("coupon", coupon));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createCoupon(@RequestBody CouponDefReq req) {
        couponService.couponDefinition(req);
        return ResponseEntity.ok(new ApiResponse<>("created", null));
    }

    @PutMapping("/{couponId}")
    public ResponseEntity<ApiResponse<Void>> updateCoupon(@PathVariable("couponId") Long couponId, @RequestBody CouponDefReq req) {
        couponService.updateCouponDefinition(couponId, req);
        return ResponseEntity.ok(new ApiResponse<>("updated", null));
    }

    @DeleteMapping("/{couponId}")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable("couponId") Long couponId) {
        couponService.deleteCouponDefinition(couponId);
        return ResponseEntity.ok(new ApiResponse<>("deleted", null));
    }
}

