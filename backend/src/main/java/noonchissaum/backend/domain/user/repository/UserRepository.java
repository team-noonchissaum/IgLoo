package noonchissaum.backend.domain.user.repository;

import noonchissaum.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {

    boolean existsByNickname(String Nickname);

    // (마이페이지) user + wallet
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.wallet WHERE u.id = :userId")
    Optional<User> findByIdWithWallet(@Param("userId") Long userId);

    // (로그인) user + auths
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.auths WHERE u.email = :email")
    Optional<User> findByEmailWithAuths(@Param("email") String email);

    //(회원가입)활성 사용자만 중복 체크
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
            "FROM User u WHERE u.email = :email AND u.status != 'DELETED'")
    boolean existsByEmailAndNotDeleted(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
            "FROM User u WHERE u.nickname = :nickname AND u.status != 'DELETED'")
    boolean existsByNicknameAndNotDeleted(@Param("nickname") String nickname);

}
