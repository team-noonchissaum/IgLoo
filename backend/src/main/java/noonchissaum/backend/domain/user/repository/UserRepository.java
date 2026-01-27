package noonchissaum.backend.domain.user.repository;

import noonchissaum.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
