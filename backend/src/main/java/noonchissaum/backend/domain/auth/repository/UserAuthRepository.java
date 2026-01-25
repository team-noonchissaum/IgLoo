package noonchissaum.backend.domain.auth.repository;

import noonchissaum.backend.domain.auth.domain.AuthType;
import noonchissaum.backend.domain.auth.domain.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserAuthRepository extends JpaRepository<UserAuth,Long>{
    Optional<UserAuth> findByAuthTypeAndIdentifier(AuthType authType, String identifier);
}
