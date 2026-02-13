package noonchissaum.backend.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    private String key(Long userId){
        return "RT:"+userId;
    }

    /**
     * 저장(기존 토큰 폐기+중복 로그인 제어)
     * */
    public void save(Long userId, String refreshToken,Long ttlSeconds){
        redisTemplate.opsForValue()
                .set(key(userId),refreshToken,ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 검증
     * */
    public boolean isValid(Long userId, String refreshToken){
        String saved=redisTemplate.opsForValue().get(key(userId));
        return refreshToken.equals(saved);
    }

    /**
     * 로그아웃/재발급시 토큰삭제
     * */
    public void delete(Long userId){
        redisTemplate.delete(key(userId));
    }
}
