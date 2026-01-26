package noonchissaum.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.auth.entity.UserAuth;
import noonchissaum.backend.domain.order.entity.Payment;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.global.entity.BaseTimeEntity;

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
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(length = 100)
    private String location;

    // 양방향 매핑: 유저의 인증 정보들
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<UserAuth> auths = new ArrayList<>();

    // 양방향 매핑: 유저의 지갑 정보 (1:1)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Wallet wallet;

    @OneToMany(mappedBy = "user",fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private final List<Payment> payments = new ArrayList<>();

    public User(String email, String nickname, UserRole role, UserStatus status) {
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
    }

    // ============비즈니스 로직 메서드============== DDD 패턴 사용!

    /**
     * 프로필 수정
     * null 이면 기본값 유지
     */

    public void updateProfile(String nickname, String profileUrl) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (profileUrl != null) {
            this.profileUrl = profileUrl;
        }
    }

    /**
     * 활성된 사용자인지 확인
     * ACTIVE 상태일 때만 트루값 반환
     */

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }
}