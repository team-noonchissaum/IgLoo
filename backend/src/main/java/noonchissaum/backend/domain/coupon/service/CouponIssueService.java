package noonchissaum.backend.domain.coupon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.coupon.dto.req.CouponIssueReq;
import noonchissaum.backend.domain.coupon.dto.res.IssuedCouponRes;
import noonchissaum.backend.domain.coupon.entity.Coupon;
import noonchissaum.backend.domain.coupon.entity.CouponIssued;
import noonchissaum.backend.domain.coupon.entity.CouponStatus;
import noonchissaum.backend.domain.coupon.repository.CouponIssuedRepository;
import noonchissaum.backend.domain.coupon.repository.CouponRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import noonchissaum.backend.domain.task.service.TaskService;
import noonchissaum.backend.domain.wallet.entity.TransactionType;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.domain.wallet.service.WalletTransactionRecordService;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.util.UserLockExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponIssueService {

    private final CouponIssuedRepository couponIssuedRepository;
    private final CouponRepository couponRepository;
    private final UserService userService;
    private final UserLockExecutor userLockExecutor;
    private final StringRedisTemplate redisTemplate;
    private final WalletTransactionRecordService walletTransactionRecordService;
    private final TaskService taskService;

    /**
     * 쿠폰 사용
     */
    @Transactional
    public void useCoupon(Long userId, Long couponIssuedId) {
        userLockExecutor.withUserLock(userId, () -> {
            if (!taskService.checkTasks(userId)) {
                throw new ApiException(ErrorCode.PENDING_TASK_EXISTS);
            }

            CouponIssued couponIssued = couponIssuedRepository.findById(couponIssuedId)
                    .orElseThrow(() -> new ApiException(ErrorCode.COUPON_NOT_FOUND));

            BigDecimal amount = couponIssued.getCoupon().getAmount();

            if (!couponIssued.getVictim().getId().equals(userId)) {
                throw new ApiException(ErrorCode.NO_AUTHORIZED_COUPON_USE);
            }

            couponIssued.use();

            Wallet wallet = couponIssued.getVictim().getWallet();

            wallet.charge(amount);

            walletTransactionRecordService.record(wallet, TransactionType.COUPON_USE, amount, couponIssuedId);

            // 사용된 쿠폰 삭제
            couponIssuedRepository.delete(couponIssued);
        });

        // Clear cache
        redisTemplate.delete(RedisKeys.userBalance(userId));
        redisTemplate.delete(RedisKeys.userLockedBalance(userId));
    }

    /**
     * 쿠폰 발급
     * 유저에게 쿠폰을 발급해줌
     * */
    @Transactional
    public void couponIssue(CouponIssueReq req) {
        List<CouponIssued> issuedList = new ArrayList<>();

        for (Long userId : req.userIds()) {
            User user = userService.getUserByUserId(userId);
            Coupon coupon = couponRepository.findById(req.couponId())
                    .orElseThrow(() -> new ApiException(ErrorCode.COUPON_NOT_FOUND));

            CouponIssued build = CouponIssued.builder()
                    .coupon(coupon)
                    .victim(user)
                    .reason(req.reason()).build();

            issuedList.add(build);
        }

        couponIssuedRepository.saveAll(issuedList);
    }

    /**
     * 발급된 쿠폰 확인 (사용된 쿠폰 제외)
     * */
    @Transactional(readOnly = true)
    public List<IssuedCouponRes> getIssuedCouponsByUserId(Long userId) {
        User user = userService.getUserByUserId(userId);

        // 사용된 쿠폰(USED)은 제외하고 조회
        List<CouponIssued> issuedCoupons = couponIssuedRepository.findByVictimAndStatusNot(
                user,
                CouponStatus.USED
        );
        List<IssuedCouponRes> resList = new ArrayList<>();

        for (CouponIssued issued : issuedCoupons) {
            IssuedCouponRes from = IssuedCouponRes.from(issued);
            resList.add(from);
        }

        return resList;
    }

    /**
     * 만료 시간 지난 쿠폰 만료처리
     * */
    @Transactional
    public void expireCoupons(){
        int updatedCount = couponIssuedRepository.expireCouponsByTime(LocalDateTime.now());
        log.info("만료된 쿠폰 {}건을 처리했습니다.", updatedCount);
    }

}
