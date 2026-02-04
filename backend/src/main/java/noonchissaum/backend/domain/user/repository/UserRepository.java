package noonchissaum.backend.domain.user.repository;

import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    boolean existsByNickname(String Nickname);

    // (마이페이지) user + wallet
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.wallet WHERE u.id = :userId")
    Optional<User> findByIdWithWallet(@Param("userId") Long userId);

    //(회원가입)활성 사용자만 중복 체크
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
            "FROM User u WHERE u.email = :email AND u.status != 'DELETED'")
    boolean existsByEmailAndNotDeleted(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
            "FROM User u WHERE u.nickname = :nickname AND u.status != 'DELETED'")
    boolean existsByNicknameAndNotDeleted(@Param("nickname") String nickname);

}
