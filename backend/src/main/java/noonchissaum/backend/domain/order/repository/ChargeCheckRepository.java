package noonchissaum.backend.domain.order.repository;

import jakarta.persistence.LockModeType;
import noonchissaum.backend.domain.order.entity.ChargeCheck;
import noonchissaum.backend.domain.order.entity.CheckStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChargeCheckRepository extends JpaRepository<ChargeCheck, Long> {

    boolean existsByPaymentId(Long paymentId);

    Long findUser_IdById(Long id);

    //비관적 락 적용한 Id로 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cc from ChargeCheck cc " +
            "join fetch cc.user " +
            "join fetch cc.payment " +
            "where cc.id = :id")
    Optional<ChargeCheck> findWithLockById(@Param("id") Long id);

    //UNCHECKED 목록 조회
    @Query("select cc from ChargeCheck cc " +
            "join cc.payment p " + // 'fetch' removed
            "where cc.user.id = :userId and cc.status = :status " +
            "order by cc.id desc")
    @EntityGraph(attributePaths = {"payment"})
    Page<ChargeCheck> findAllByUserIdAndStatusFetchPayment(@Param("userId") Long userId,
                                                           @Param("status") CheckStatus status,
                                                           Pageable pageable);

    // 7일 지난 UNCHECKED id 목록

    @Query("select cc.id from ChargeCheck cc " +
            "where cc.status = :status and cc.createdAt <= :threshold")
    List<Long> findExpiredUncheckedIds(@Param("status") CheckStatus status,
                                       @Param("threshold") LocalDateTime threshold,
                                       Pageable pageable);
}
