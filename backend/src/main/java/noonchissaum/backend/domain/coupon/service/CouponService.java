package noonchissaum.backend.domain.coupon.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.coupon.dto.req.CouponDefReq;
import noonchissaum.backend.domain.coupon.dto.res.CouponRes;
import noonchissaum.backend.domain.coupon.entity.Coupon;
import noonchissaum.backend.domain.coupon.repository.CouponRepository;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    /**
     * 쿠폰 생성
     * 특정 사유로 사용될 쿠폰을 정의 (발급 x)
     * */
    @Transactional
    public void couponDefinition(CouponDefReq req) {
        Coupon coupon = Coupon.builder()
                .name(req.name())
                .amount(req.amount())
                .build();

        couponRepository.save(coupon);
    }

    /**
     * 쿠폰 삭제
     * */
    @Transactional
    public void deleteCouponDefinition(Long couponId) {
        if (!couponRepository.existsById(couponId)) {
            throw new ApiException(ErrorCode.COUPON_NOT_FOUND);
        }
        couponRepository.deleteById(couponId);
    }

    @Transactional(readOnly = true)
    public List<CouponRes> getAllCoupons() {
        List<Coupon> coupons = couponRepository.findAll();
        List<CouponRes> resList = new ArrayList<>();

        for (Coupon coupon : coupons) {
            CouponRes res = new CouponRes(
                    coupon.getId(),
                    coupon.getName(),
                    coupon.getAmount()
            );
            resList.add(res);
        }

        return resList;
    }

    @Transactional(readOnly = true)
    public CouponRes getCouponById(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ApiException(ErrorCode.COUPON_NOT_FOUND));

        return new CouponRes(
                coupon.getId(),
                coupon.getName(),
                coupon.getAmount()
        );
    }

    @Transactional
    public void updateCouponDefinition(Long couponId, CouponDefReq req) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ApiException(ErrorCode.COUPON_NOT_FOUND));

        coupon.updateCoupon(req.name(), req.amount());
        couponRepository.save(coupon);
    }
}
