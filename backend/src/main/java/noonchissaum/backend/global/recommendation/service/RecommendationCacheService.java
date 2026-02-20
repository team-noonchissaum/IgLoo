package noonchissaum.backend.global.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RecommendationCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper; // JSON serializer/deserializer

    private static final String RECOMMENDATION_KEY_PREFIX = "recommendations:user:";
    private static final long RECOMMENDATION_TTL_MINUTES = 30; // cache TTL

    public void saveUserRecommendations(Long userId, List<Long> recommendedItemIds) {
        String key = RECOMMENDATION_KEY_PREFIX + userId;
        try {
            String jsonRecommendedItemIds = objectMapper.writeValueAsString(recommendedItemIds);
            stringRedisTemplate.opsForValue().set(key, jsonRecommendedItemIds, RECOMMENDATION_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize recommendations for userId=" + userId + ": " + e.getMessage());
        }
    }

    public List<Long> getUserRecommendations(Long userId) {
        String key = RECOMMENDATION_KEY_PREFIX + userId;
        String jsonRecommendedItemIds = stringRedisTemplate.opsForValue().get(key);

        if (jsonRecommendedItemIds == null) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(jsonRecommendedItemIds,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
        } catch (JsonProcessingException e) {
            System.err.println("Failed to deserialize recommendations for userId=" + userId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }
}