package noonchissaum.backend.domain.user.service;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    }
    public User getSeller(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
