package noonchissaum.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.auth.entity.UserAuth;
import noonchissaum.backend.domain.order.entity.ChargeCheck;
import noonchissaum.backend.domain.order.entity.Payment;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.global.entity.BaseTimeEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(name = "profile_url", length = 500)
    private String profileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(length = 100)
    private String location;

    private LocalDateTime deletedAt;

    private LocalDateTime blockedAt;

    private String blockReason;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<UserAuth> auths = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Wallet wallet;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private final List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ChargeCheck> chargeChecks = new ArrayList<>();

    @Builder
    public User(String email, String nickname, UserRole role, UserStatus status) {
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
    }

    public static User createLocalUser(String email, String nickname) {
        return new User(email, nickname, UserRole.USER, UserStatus.ACTIVE);
    }

    // ============ 비즈니스 로직 ============

    /**
     * 프로필 수정
     */
    public void updateProfile(String nickname, String profileUrl) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        this.profileUrl = profileUrl; // 이미지 삭제시 null
    }

    /**
     * 활성된 사용자인지 확인
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    /**
     * 회원 탈퇴 (soft delete)
     */
    public void softDelete() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    // =========== 관리자 기능 ==========

    /**
     * 사용자 차단
     */
    public void block(String reason) {
        if (this.status == UserStatus.BLOCKED) {
            throw new IllegalArgumentException("이미 차단된 사용자입니다.");
        }
        this.status = UserStatus.BLOCKED;
        this.blockedAt = LocalDateTime.now();
        this.blockReason = reason;
    }

    /**
     * 차단 해제
     */
    public void unblock() {
        if (this.status != UserStatus.BLOCKED) {
            throw new IllegalArgumentException("차단된 사용자가 아닙니다");
        }
        this.status = UserStatus.ACTIVE;
        this.blockedAt = null;
        this.blockReason = null;
    }

    public void registWallet(Wallet wallet) {
        this.wallet = wallet;
    }
}
