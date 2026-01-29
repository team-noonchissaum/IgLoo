package noonchissaum.backend.domain.order.entity;

import jakarta.persistence.*;
import lombok.*;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.entity.BaseTimeEntity;

@Entity
@Table(name = "charge_checks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChargeCheck extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="user_id", nullable=false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", unique = true)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    private CheckStatus status = CheckStatus.UNCHECKED;

    @Builder
    public ChargeCheck(Payment payment) {
        this.user = payment.getUser();
        this.payment = payment;
    }

    public void changeStatus(CheckStatus status){
        this.status = status;
    }
}
