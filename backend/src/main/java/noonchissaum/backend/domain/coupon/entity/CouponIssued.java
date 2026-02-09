package noonchissaum.backend.domain.coupon.entity;

import jakarta.persistence.*;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_issued")
public class CouponIssued extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Enumerated(EnumType.STRING)
    private CouponStatus status;

    private String reason;

    private LocalDateTime expiredAt;
}
