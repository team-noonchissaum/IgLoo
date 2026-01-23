package noonchissaum.backend.domain.wallet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.global.entity.BaseTimeEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wallets")
@Getter
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

    // 양방향 매핑: 지갑 거래 내역
    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL)
    private List<WalletTransaction> transactions = new ArrayList<>();

    // 양방향 매핑: 출금 신청 내역
    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL)
    private List<Withdrawal> withdrawals = new ArrayList<>();

    public Wallet(User user) {
        this.user = user;
        this.balance = BigDecimal.ZERO;
    }
}