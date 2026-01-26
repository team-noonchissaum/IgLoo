package noonchissaum.backend.domain.user.service;

import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@NoArgsConstructor
public class UserService {
    private UserRepository userRepository;

    public User getSeller(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
