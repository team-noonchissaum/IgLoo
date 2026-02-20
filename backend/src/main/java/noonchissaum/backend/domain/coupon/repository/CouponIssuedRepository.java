package noonchissaum.backend.domain.coupon.repository;

import noonchissaum.backend.domain.coupon.entity.CouponIssued;
import noonchissaum.backend.domain.coupon.entity.CouponStatus;
import noonchissaum.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponIssuedRepository extends JpaRepository<CouponIssued, Long> {

    @EntityGraph(attributePaths = {"coupon"})
    List<CouponIssued> findByVictim(User victim);

    //사용한 쿠폰 삭제
    @EntityGraph(attributePaths = {"coupon"})
    List<CouponIssued> findByVictimAndStatusNot(User victim, CouponStatus status);

    @Modifying
    @Query("UPDATE CouponIssued c set c.status = 'EXPIRED' " +
            "WHERE c.status = 'UNUSED' AND c.expiredAt < :now")
    int expireCouponsByTime(@Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"coupon", "victim", "victim.wallet"})
    Optional<CouponIssued> findById(Long id);
}
