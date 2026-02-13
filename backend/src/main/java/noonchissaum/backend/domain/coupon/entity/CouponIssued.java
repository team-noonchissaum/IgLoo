package noonchissaum.backend.domain.coupon.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.entity.BaseTimeEntity;

import java.time.LocalDate;

@Entity
@Table(name = "coupon_issued")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssued extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User victim;

    @Enumerated(EnumType.STRING)
    private CouponStatus status = CouponStatus.UNUSED;

    private String reason;

    private LocalDate expiredAt = LocalDate.now().plusMonths(1);

    @Builder
    public CouponIssued(Coupon coupon, User victim, String reason) {
        this.coupon = coupon;
        this.victim = victim;
        this.reason = reason;
    }

    public void use() {
        if (this.status != CouponStatus.UNUSED) {
            throw new IllegalStateException("이미 사용되었거나 만료된 쿠폰입니다.");
        }
        this.status = CouponStatus.USED;
    }

    public void expire() {
        this.status = CouponStatus.EXPIRED;
    }
}
