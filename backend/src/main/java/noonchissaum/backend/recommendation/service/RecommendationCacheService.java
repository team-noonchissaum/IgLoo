package noonchissaum.backend.recommendation.service;

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
    private final ObjectMapper objectMapper; // JSON 직렬화/역직렬화를 위한 ObjectMapper

    private static final String RECOMMENDATION_KEY_PREFIX = "recommendations:user:";
    private static final long RECOMMENDATION_TTL_MINUTES = 30; // 추천 캐시 TTL

    /**
     * 특정 사용자를 위한 추천 항목 ID 목록을 Redis에 저장합니다.
     * 캐시 키는 `recommendations:user:<userId>` 형식입니다.
     *
     * @param userId 추천 대상 사용자 ID.
     * @param recommendedItemIds 추천 항목 ID 목록.
     */
    public void saveUserRecommendations(Long userId, List<Long> recommendedItemIds) {
        String key = RECOMMENDATION_KEY_PREFIX + userId;
        try {
            String jsonRecommendedItemIds = objectMapper.writeValueAsString(recommendedItemIds);
            stringRedisTemplate.opsForValue().set(key, jsonRecommendedItemIds, RECOMMENDATION_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            System.err.println("사용자" + userId + "의 추천 직렬화 오류: " + e.getMessage());
        }
    }

    /**
     * Redis 캐시에서 주어진 사용자에 대한 추천 항목 ID 목록을 검색합니다.
     *
     * @param userId 추천을 검색할 사용자 ID.
     * @return 추천 항목 ID 목록, 없거나 오류 발생 시 빈 목록.
     */
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
            System.err.println("사용자" + userId + "의 추천 역직렬화 오류: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}