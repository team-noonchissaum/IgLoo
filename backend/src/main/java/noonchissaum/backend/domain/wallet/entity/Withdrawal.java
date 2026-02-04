package noonchissaum.backend.domain.wallet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdrawal_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(precision = 15, scale = 0)
    private BigDecimal amount;

    @Column(name = "fee_amount", precision = 15, scale = 0)
    private BigDecimal feeAmount;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_number")
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalStatus status;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public static Withdrawal create(
            Wallet wallet,
            BigDecimal amount,
            BigDecimal feeAmount,
            String bankName,
            String accountNumber
    ) {
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.wallet = wallet;
        withdrawal.amount = amount;
        withdrawal.feeAmount = feeAmount;
        withdrawal.bankName = bankName;
        withdrawal.accountNumber = accountNumber;
        withdrawal.status = WithdrawalStatus.REQUESTED;
        return withdrawal;
    }

    public void confirm() {
        this.status = WithdrawalStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
    }
    public void reject() {
        this.status = WithdrawalStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
    }
}
