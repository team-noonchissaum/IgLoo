package noonchissaum.backend.domain.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.user.entity.User;

@Entity
@Table(name = "charge_checks")
@Getter
public class ChargeCheck {
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
}
