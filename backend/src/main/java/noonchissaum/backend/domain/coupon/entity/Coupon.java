package noonchissaum.backend.domain.coupon.entity;

import jakarta.persistence.*;
import noonchissaum.backend.global.entity.BaseTimeEntity;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "coupons")
public class Coupon extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;

    private String name;

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CouponIssued> couponIssued;
}
