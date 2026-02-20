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
@Table(name = "wallet_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(precision = 15, scale = 0,nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", length = 20)
    private TransactionRefType refType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(length = 255)
    private String memo;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


    public static WalletTransaction create(
            Wallet wallet,
            BigDecimal amount,
            TransactionType type,
            Long refId
    ) {

        WalletTransaction tx = new WalletTransaction();
        tx.wallet = wallet;
        tx.amount = amount;
        tx.type = type;
        tx.refType = type.getDefaultRefType();
        tx.refId = refId;
        tx.memo = type.getMemo();

        return tx;
    }

    public void confirmWithdrawal(){
        TransactionType type = TransactionType.WITHDRAW_CONFIRM;
        this.type = type;
        this.refType = type.getDefaultRefType();
        this.memo = type.getMemo();
    }
}
