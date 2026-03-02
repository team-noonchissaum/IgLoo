package noonchissaum.backend.domain.user.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.global.RedisKeys;
import noonchissaum.backend.global.security.SecurityUserCache;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final Duration TTL = Duration.ofMinutes(5);

    public SecurityUserCache getUser(Long userId) {

        String key = RedisKeys.userCache(userId);
        String cached = redisTemplate.opsForValue().get(key);

        if (cached != null) {
            return deserialize(cached);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        SecurityUserCache dto = new SecurityUserCache(user);

        redisTemplate.opsForValue().set(key, serialize(dto), TTL);

        return dto;
    }
    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Redis serialize error", e);
        }
    }

    private SecurityUserCache deserialize(String json) {
        try {
            return objectMapper.readValue(json, SecurityUserCache.class);
        } catch (Exception e) {
            throw new RuntimeException("Redis deserialize error", e);
        }
    }

    public void evict(Long userId) {
        redisTemplate.delete(RedisKeys.userCache(userId));
    }
}