package noonchissaum.backend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.global.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private static final long PASSWORD_RESET_TOKEN_TTL_SECONDS = 60L * 15;

    private final StringRedisTemplate redisTemplate;

    public String createToken(Long userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue()
                .set(RedisKeys.passwordResetToken(token), String.valueOf(userId), PASSWORD_RESET_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);
        return token;
    }

    public Long getUserIdByToken(String token) {
        String value = redisTemplate.opsForValue().get(RedisKeys.passwordResetToken(token));
        if (value == null) {
            return null;
        }
        return Long.parseLong(value);
    }

    public void deleteToken(String token) {
        redisTemplate.delete(RedisKeys.passwordResetToken(token));
    }
}
