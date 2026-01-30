package noonchissaum.backend.domain.auction.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.dto.ws.AuctionSnapshotPayload;
import noonchissaum.backend.global.RedisKeys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuctionRealtimeSnapshotService {

    private final StringRedisTemplate redisTemplate;
    private final AuctionRedisService auctionRedisService;

    public AuctionSnapshotPayload getSnapshot(Long auctionId) {
        String priceKey = RedisKeys.auctionCurrentPrice(auctionId);
        String bidderKey = RedisKeys.auctionCurrentBidder(auctionId);
        String bidCountKey = RedisKeys.auctionCurrentBidCount(auctionId);
        String endTimeKey = RedisKeys.auctionEndTime(auctionId);
        String imminentKey = RedisKeys.auctionImminentMinutes(auctionId);
        String extendedKey = RedisKeys.auctionIsExtended(auctionId);

        // 키가 하나라도 없으면 복구
        if (!hasAllKeys(priceKey, bidderKey, bidCountKey, endTimeKey, imminentKey, extendedKey)) {
            auctionRedisService.setRedis(auctionId);
        }

        String rawPrice = redisTemplate.opsForValue().get(priceKey);
        String rawBidder = redisTemplate.opsForValue().get(bidderKey);
        String rawBidCount = redisTemplate.opsForValue().get(bidCountKey);
        String rawEndTime = redisTemplate.opsForValue().get(endTimeKey);
        String rawImminent = redisTemplate.opsForValue().get(imminentKey);
        String rawExtended = redisTemplate.opsForValue().get(extendedKey);

        Long currentPrice = parseLongOrNull(rawPrice);
        Integer bidCount = parseIntOrNull(rawBidCount);

        // bidder는 공백이면 null 처리
        Long currentBidderId = parseBidderId(rawBidder);

        LocalDateTime endAt = parseEndTime(rawEndTime);
        Integer imminentMinutes = parseIntOrNull(rawImminent);
        Boolean isExtended = parseBooleanOrNull(rawExtended);

        return new AuctionSnapshotPayload(
                auctionId,
                currentPrice != null ? currentPrice : 0L,
                currentBidderId,
                bidCount != null ? bidCount : 0,
                endAt,
                imminentMinutes,
                isExtended
        );
    }

    private boolean hasAllKeys(String... keys) {
        for (String key : keys) {
            if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
                return false;
            }
        }
        return true;
    }

    private Long parseBidderId(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        try {
            long id = Long.parseLong(v);
            return (id <= 0) ? null : id;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLongOrNull(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseIntOrNull(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * endTime은 저장 포맷이 달라질 수 있어서 방어적으로 파싱:
     * 1) epoch millis (숫자) -> LocalDateTime
     * 2) LocalDateTime.parse (예: 2026-01-29T11:05:00)
     */
    private LocalDateTime parseEndTime(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;

        // epoch millis case
        if (v.chars().allMatch(Character::isDigit)) {
            long epochMillis = Long.parseLong(v);
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.of("Asia/Seoul"));
        }

        // ISO LocalDateTime case
        return LocalDateTime.parse(v);
    }

    private Boolean parseBooleanOrNull(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.isEmpty()) return null;
        // true/false or 1/0 방어
        if ("1".equals(v)) return true;
        if ("0".equals(v)) return false;
        return Boolean.parseBoolean(v);
    }

    public Optional<AuctionSnapshotPayload> getSnapshotIfPresent(Long auctionId) {
        String priceKey = RedisKeys.auctionCurrentPrice(auctionId);
        String bidderKey = RedisKeys.auctionCurrentBidder(auctionId);
        String bidCountKey = RedisKeys.auctionCurrentBidCount(auctionId);
        String endTimeKey = RedisKeys.auctionEndTime(auctionId);
        String imminentKey = RedisKeys.auctionImminentMinutes(auctionId);
        String extendedKey = RedisKeys.auctionIsExtended(auctionId);

        // 하나라도 없으면 "없다"로 처리 (복구 X)
        if (!hasAllKeys(priceKey, bidderKey, bidCountKey, endTimeKey, imminentKey, extendedKey)) {
            return Optional.empty();
        }

        //
        return Optional.of(getSnapshot(auctionId));
    }
}
