package noonchissaum.backend.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.entity.BaseTimeEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    @Column(precision = 15, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.REQUEST;

    @Column(name = "paid_at")
    private LocalDateTime paidAt = LocalDateTime.now();

    @Column(name = "pg_order_id")
    private String pgOrderId;

    @Column(name = "payment_key")
    private String paymentKey;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "pg_provider")
    private PgProvider pgProvider = PgProvider.TOSS;

    @Column(name = "bank")
    private String bank;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "due_date")
    private String dueDate;

    @OneToOne(mappedBy = "payment")
    private ChargeCheck chargeCheck;

    @Builder
    public Payment(User user, BigDecimal amount, PgProvider pgProvider, String pgOrderId) {
        this.user = user;
        this.amount = amount;
        this.pgProvider = pgProvider;
        this.pgOrderId = pgOrderId;
    }

    public void approve(String paymentKey){
        if (this.status != PaymentStatus.REQUEST && this.status != PaymentStatus.WAITING_FOR_DEPOSIT){
            throw new IllegalStateException("CREATED 상태에서만 결제 승인 가능");
        }
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void abort(String reason){
        if (this.status != PaymentStatus.REQUEST){
            throw new IllegalStateException("CREATED 상태에서만 결제 승인 가능");
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void cancel(){
        if (this.status != PaymentStatus.APPROVED){
            throw new IllegalStateException("DONE 상태에서만 결제 승인 가능");
        }
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    public void waitDeposit(String paymentKey, String bank, String accountNumber, String dueDate) {
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.WAITING_FOR_DEPOSIT;
        this.bank = bank;
        this.accountNumber = accountNumber;
        this.dueDate = dueDate;
    }
}
