package noonchissaum.backend.domain.coupon.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.global.entity.BaseTimeEntity;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Coupon extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;

    @Column(unique = true)
    private String name;

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CouponIssued> couponIssued;

    public void updateCoupon(String name, BigDecimal amount) {
        this.name = name;
        this.amount = amount;
    }

    @Builder
    public Coupon(BigDecimal amount, String name) {
        this.amount = amount;
        this.name = name;
    }
}
