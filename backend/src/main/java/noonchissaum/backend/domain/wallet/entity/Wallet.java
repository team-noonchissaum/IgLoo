package noonchissaum.backend.domain.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.entity.BaseTimeEntity;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    // 양방향 매핑: 지갑 거래 내역
    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL)
    private List<WalletTransaction> transactions = new ArrayList<>();

    // 양방향 매핑: 출금 신청 내역
    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL)
    private List<Withdrawal> withdrawals = new ArrayList<>();

    public Wallet(User user) {
        this.user = user;
    }

    @Builder
    // 테스트용 wallet 생성기
    public Wallet(User user, BigDecimal balance, BigDecimal lockedBalance) {
        this.user = user;
        this.balance = balance;
        this.lockedBalance = lockedBalance;
    }

    public void bidCanceled(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.lockedBalance = this.lockedBalance.subtract(amount);
    }

    public void bid(BigDecimal amount) {
        if (this.balance.compareTo(amount) <= 0) {
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.balance = this.balance.subtract(amount);
        this.lockedBalance = this.lockedBalance.add(amount);
    }
    public void setLockedBalance(BigDecimal lockedBalance) {
        this.lockedBalance = lockedBalance;
    }

    public void auctionDeposit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public void auctionRefund(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void charge(BigDecimal amount){
        this.balance = this.balance.add(amount);
    }

    public void withdrawRequest(BigDecimal total) {
        if (this.balance.compareTo(total) < 0) {
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.balance = this.balance.subtract(total);
        this.lockedBalance = this.lockedBalance.add(total);
    }

    public void withdrawRollback(BigDecimal total) {
        this.balance = this.balance.add(total);
        this.lockedBalance = this.lockedBalance.subtract(total);
    }

    public void withdrawConfirm(BigDecimal total) {
        this.lockedBalance = this.lockedBalance.subtract(total);
    }

    // 정산 메서드
    public void releaseLocked(BigDecimal amount){
        if (this.lockedBalance.compareTo(amount) < 0) {
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.lockedBalance = this.lockedBalance.subtract(amount);
    }

    public void addBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}