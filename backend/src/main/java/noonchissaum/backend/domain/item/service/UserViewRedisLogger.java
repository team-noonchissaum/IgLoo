package noonchissaum.backend.domain.item.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.global.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserViewRedisLogger {

    private final StringRedisTemplate stringRedisTemplate;

    // keep views for 24 hours
    private static final long USER_VIEWS_TTL_HOURS = 24;

    // trending bucket key format: yyyyMMddHH
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final long TRENDING_TTL_HOURS = 2;

    public void logView(Long userId, Long itemId) {
        if (userId == null || itemId == null) {
            return;
        }

        String key = RedisKeys.userViews(userId);

        // append item view
        stringRedisTemplate.opsForList().rightPush(key, String.valueOf(itemId));

        // extend TTL on each view
        stringRedisTemplate.expire(key, USER_VIEWS_TTL_HOURS, TimeUnit.HOURS);

        // trending counter (hour bucket)
        String hourKey = HOUR_FORMATTER.format(LocalDateTime.now());
        String trendingKey = RedisKeys.itemViewsHour(hourKey);
        stringRedisTemplate.opsForZSet().incrementScore(trendingKey, String.valueOf(itemId), 1.0);
        stringRedisTemplate.expire(trendingKey, TRENDING_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * Returns all viewed item IDs within TTL.
     */
    public List<String> getUserViewedItems(Long userId) {
        String key = RedisKeys.userViews(userId);
        return stringRedisTemplate.opsForList().range(key, 0, -1);
    }
}