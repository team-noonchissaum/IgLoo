package noonchissaum.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.auth.entity.UserAuth;
import noonchissaum.backend.domain.coupon.entity.CouponIssued;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.order.entity.ChargeCheck;
import noonchissaum.backend.domain.order.entity.Payment;
import noonchissaum.backend.domain.wallet.entity.Wallet;
import noonchissaum.backend.global.entity.BaseTimeEntity;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.hibernate.annotations.ColumnTransformer;
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
    private String address; // 사용자 전체주소(비 노출용)

    @Column(length = 50)
    private String dong; // 노출주소

    // 추가: 위치 정보 필드
    @Column(columnDefinition = "DOUBLE")
    private Double latitude;

    @Column(columnDefinition = "DOUBLE")
    private Double longitude;

    @Column(columnDefinition = "POINT SRID 4326", name = "location")
    @ColumnTransformer(
            read = "ST_AsText(location)",
            write = "ST_GeomFromText(?, 4326)"
    )
    private String location; // WKT 형식: "POINT(latitude longitude)"


    private LocalDateTime deletedAt;

    private LocalDateTime blockedAt;

    private String blockReason;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserAuth userAuth;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Wallet wallet;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private final List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ChargeCheck> chargeChecks = new ArrayList<>();

    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Item> items = new ArrayList<>();

    @OneToMany(mappedBy = "victim", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CouponIssued> issuedList = new ArrayList<>();

    @Builder
    public User(String email, String nickname, UserRole role, UserStatus status) {
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
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
    /** 위치 정보 업데이트(주소,위도 ,경도)*/
    public void updateLocation(String address, String dong, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new ApiException(ErrorCode.SET_LOCATION_ERROR);
        }
        this.address = address;
        this.dong = dong;
        this.latitude = latitude;
        this.longitude = longitude;
        // WKT 형식으로 저장 (위도, 경도 순서)
        this.location = String.format("POINT(%f %f)", latitude, longitude);
    }


    /**
     * 활성된 사용자인지 확인
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    // =========== 관리자 기능 ==========

    /**
     * 사용자 차단
     */
    public void block(String reason) {
        if (this.status == UserStatus.BLOCKED) {
            throw new CustomException(ErrorCode.USER_ALREADY_BLOCKED);
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
            throw new ApiException(ErrorCode.USER_NOT_BLOCKED);
        }
        this.status = UserStatus.ACTIVE;
        this.blockedAt = null;
        this.blockReason = null;
    }

    public void registerWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public void delete(){
        if(this.status==UserStatus.DELETED){
            throw new ApiException(ErrorCode.USER_ALREADY_DELETED);
        }
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    //test용
    public void assignId(Long id){
        this.id=id;
;    }
}
