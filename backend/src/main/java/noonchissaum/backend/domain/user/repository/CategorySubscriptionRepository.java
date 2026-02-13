package noonchissaum.backend.domain.user.repository;

import noonchissaum.backend.domain.user.entity.CategorySubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategorySubscriptionRepository extends JpaRepository<CategorySubscription, Long> {

    /** 유저의 관심 카테고리 목록 조회 (카테고리 fetch join) */
    @Query("""
            select cs
            from CategorySubscription cs
            join fetch cs.category c
            where cs.user.id = :userId
            order by c.id asc
            """)
    List<CategorySubscription> findAllByUserId(@Param("userId") Long userId);

    /** 유저의 관심 카테고리 전체 삭제 */
    void deleteAllByUser_Id(Long userId);

    boolean existsByUser_IdAndCategory_Id(Long userId, Long categoryId);

    void deleteByUser_IdAndCategory_Id(Long userId, Long categoryId);

    /** 특정 카테고리를 구독 중인 활성 유저 이메일 목록 조회 */
    @Query("""
            select distinct u.email
            from CategorySubscription cs
            join cs.user u
            where cs.category.id = :categoryId
              and u.status = noonchissaum.backend.domain.user.entity.UserStatus.ACTIVE
            """)
    List<String> findActiveUserEmailsByCategoryId(@Param("categoryId") Long categoryId);
}
