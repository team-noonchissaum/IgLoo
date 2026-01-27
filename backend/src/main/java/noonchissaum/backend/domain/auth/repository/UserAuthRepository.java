package noonchissaum.backend.domain.auth.repository;

import noonchissaum.backend.domain.auth.entity.AuthType;
import noonchissaum.backend.domain.auth.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserAuthRepository extends JpaRepository<UserAuth,Long>{
    /**로그인용 조회(Local/OAuth)*/
    Optional<UserAuth> findByAuthTypeAndIdentifier(AuthType authType,String identifier);

    /**회원 가입시 중복 체크용*/
    boolean existsByIdentifierAndAuthType(AuthType authType,String identifier);
}
