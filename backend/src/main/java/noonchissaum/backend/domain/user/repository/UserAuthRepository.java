package noonchissaum.backend.domain.user.repository;

import noonchissaum.backend.domain.user.entity.AuthType;
import noonchissaum.backend.domain.user.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserAuthRepository extends JpaRepository<UserAuth,Long>{
    Optional<UserAuth> findByAuthTypeAndIdentifier(AuthType authType,String identifier);
}
