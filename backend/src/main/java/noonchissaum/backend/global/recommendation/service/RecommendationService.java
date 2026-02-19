package noonchissaum.backend.global.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.dto.res.AuctionRes;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.item.service.WishService;
import noonchissaum.backend.global.RedisKeys;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final RecommendationCacheService recommendationCacheService;
    private final AuctionRepository auctionRepository;
    private final WishService wishService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 5;
    private static final int RECOMMENDATION_POOL_SIZE = 30;
    private static final int MAX_VIEWS_PER_USER = 200;
    private static final int MIN_VIEWS_FOR_SIMILARITY = 5;
    private static final int TOP_K_USERS = 50;
    private static final int TRENDING_FETCH_MULTIPLIER = 3;
    private static final int NEW_ITEMS_TARGET = 1;
    private static final int NEW_ITEMS_LOOKBACK_MINUTES = 30;
    private static final Set<AuctionStatus> RECOMMENDATION_ALLOWED_STATUSES =
            Set.of(AuctionStatus.RUNNING, AuctionStatus.DEADLINE);

    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final Random RANDOM = new Random();

    /**
     * Returns recommended auctions for a user. Falls back to trending if similarity is empty.
     */
    public List<AuctionRes> getRecommendedAuctions(Long userId, Long contextItemId, Long contextAuctionId) {
        List<Long> cachedRecommendedItemIds = recommendationCacheService.getUserRecommendations(userId);
        if (!cachedRecommendedItemIds.isEmpty()) {
            List<Long> filtered = filterOutContext(cachedRecommendedItemIds, contextItemId);
            List<Long> filled = fillWithTrendingIfNeeded(userId, contextItemId, filtered, RECOMMENDATION_POOL_SIZE);
            return finalizeRecommendations(userId, contextItemId, contextAuctionId, filled);
        }

        List<Long> recommendedItemIds = recommendBySimilarity(userId, RECOMMENDATION_POOL_SIZE);
        recommendedItemIds = filterItemPoolByAllowedStatus(recommendedItemIds, RECOMMENDATION_POOL_SIZE);
        if (recommendedItemIds.isEmpty()) {
            Set<Long> viewed = new HashSet<>(getUserViews(userId));
            recommendedItemIds = getTrendingItems(RECOMMENDATION_POOL_SIZE, viewed);
            recommendedItemIds = filterItemPoolByAllowedStatus(recommendedItemIds, RECOMMENDATION_POOL_SIZE);
        }

        if (recommendedItemIds.isEmpty()) {
            return Collections.emptyList();
        }

        recommendationCacheService.saveUserRecommendations(userId, recommendedItemIds);
        List<Long> filtered = filterOutContext(recommendedItemIds, contextItemId);
        List<Long> filled = fillWithTrendingIfNeeded(userId, contextItemId, filtered, RECOMMENDATION_POOL_SIZE);
        return finalizeRecommendations(userId, contextItemId, contextAuctionId, filled);
    }

    private List<Long> recommendBySimilarity(Long userId, int limit) {
        List<Long> viewsU = getUserViews(userId);
        Set<Long> viewsUSet = new HashSet<>(viewsU);
        if (viewsUSet.size() < MIN_VIEWS_FOR_SIMILARITY) {
            return new ArrayList<>();
        }

        List<Long> candidateUserIds = scanUserIds();
        candidateUserIds.remove(userId);

        List<UserSimilarity> sims = new ArrayList<>();
        for (Long uid : candidateUserIds) {
            List<Long> viewsV = getUserViews(uid);
            if (viewsV.isEmpty()) {
                continue;
            }
            double sim = jaccard(viewsUSet, new HashSet<>(viewsV));
            if (sim >= 0.2) {
                sims.add(new UserSimilarity(uid, sim));
            }
        }

        if (sims.isEmpty()) {
            return new ArrayList<>();
        }

        sims.sort(Comparator.comparingDouble(UserSimilarity::similarity).reversed());
        if (sims.size() > TOP_K_USERS) {
            sims = sims.subList(0, TOP_K_USERS);
        }

        Map<Long, Double> scores = new HashMap<>();
        for (UserSimilarity sim : sims) {
            List<Long> viewsV = getUserViews(sim.userId());
            Set<Long> viewsVSet = new HashSet<>(viewsV);
            for (Long itemId : viewsVSet) {
                if (viewsUSet.contains(itemId)) {
                    continue;
                }
                scores.merge(itemId, sim.similarity(), Double::sum);
            }
        }

        if (scores.size() < RECOMMENDATION_POOL_SIZE) {
            // fallback: allow already-viewed items to expand the pool
            for (UserSimilarity sim : sims) {
                List<Long> viewsV = getUserViews(sim.userId());
                Set<Long> viewsVSet = new HashSet<>(viewsV);
                for (Long itemId : viewsVSet) {
                    scores.merge(itemId, sim.similarity(), Double::sum);
                }
            }
        }

        if (scores.isEmpty()) {
            return new ArrayList<>();
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Long> getTrendingItems(int limit, Set<Long> exclude) {
        LocalDateTime now = LocalDateTime.now();
        String keyNow = RedisKeys.itemViewsHour(HOUR_FORMATTER.format(now));
        String keyPrev = RedisKeys.itemViewsHour(HOUR_FORMATTER.format(now.minusHours(1)));

        Map<Long, Double> scores = new HashMap<>();
        accumulateTrending(scores, keyNow, limit * TRENDING_FETCH_MULTIPLIER);
        accumulateTrending(scores, keyPrev, limit * TRENDING_FETCH_MULTIPLIER);

        if (scores.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> ranked = scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));

        if (exclude != null && !exclude.isEmpty()) {
            List<Long> filtered = ranked.stream()
                    .filter(id -> !exclude.contains(id))
                    .collect(Collectors.toCollection(ArrayList::new));
            if (!filtered.isEmpty()) {
                ranked = filtered;
            }
        }

        if (ranked.size() > limit) {
            return new ArrayList<>(ranked.subList(0, limit));
        }
        return ranked;
    }

    private void accumulateTrending(Map<Long, Double> scores, String key, int fetchLimit) {
        Set<ZSetOperations.TypedTuple<String>> items = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(key, 0, fetchLimit - 1);
        if (items == null || items.isEmpty()) {
            return;
        }
        for (ZSetOperations.TypedTuple<String> tuple : items) {
            String value = tuple.getValue();
            Double score = tuple.getScore();
            if (value == null || score == null) {
                continue;
            }
            try {
                Long itemId = Long.parseLong(value);
                scores.merge(itemId, score, Double::sum);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private List<Long> getUserViews(Long userId) {
        String key = RedisKeys.userViews(userId);
        List<String> values = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        if (values.size() > MAX_VIEWS_PER_USER) {
            values = values.subList(values.size() - MAX_VIEWS_PER_USER, values.size());
        }
        List<Long> out = new ArrayList<>();
        for (String v : values) {
            try {
                out.add(Long.parseLong(v));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    private List<Long> scanUserIds() {
        List<Long> userIds = new ArrayList<>();
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions().match(RedisKeys.userViewsPattern()).count(1000).build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String[] parts = key.split(":");
                if (parts.length != 3) {
                    continue;
                }
                if (!"user".equals(parts[0]) || !"views".equals(parts[2])) {
                    continue;
                }
                try {
                    userIds.add(Long.parseLong(parts[1]));
                } catch (NumberFormatException ignored) {
                    log.error("숫자 에러");
                }
            }
        }
        return userIds;
    }

    private double jaccard(Set<Long> a, Set<Long> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 0.0;
        }
        Set<Long> small = a.size() <= b.size() ? a : b;
        Set<Long> large = a.size() <= b.size() ? b : a;
        int intersection = 0;
        for (Long v : small) {
            if (large.contains(v)) {
                intersection++;
            }
        }
        int union = a.size() + b.size() - intersection;
        if (union == 0) {
            return 0.0;
        }
        return (double) intersection / union;
    }

    private List<Long> filterOutContext(List<Long> items, Long contextItemId) {
        if (items == null || items.isEmpty() || contextItemId == null) {
            return items == null ? new ArrayList<>() : new ArrayList<>(items);
        }
        return items.stream()
                .filter(id -> !contextItemId.equals(id))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Long> fillWithTrendingIfNeeded(Long userId, Long contextItemId, List<Long> base, int limit) {
        if (base == null) {
            base = new ArrayList<>();
        }
        base = filterItemPoolByAllowedStatus(base, limit);
        if (base.size() >= limit) {
            return new ArrayList<>(base.subList(0, limit));
        }
        Set<Long> exclude = new HashSet<>(getUserViews(userId));
        if (contextItemId != null) {
            exclude.add(contextItemId);
        }
        exclude.addAll(base);
        List<Long> trending = getTrendingItems(limit + exclude.size(), exclude);
        trending = filterItemPoolByAllowedStatus(trending, limit + exclude.size());
        List<Long> merged = new ArrayList<>(base);
        for (Long id : trending) {
            if (merged.size() >= limit) {
                break;
            }
            if (!exclude.contains(id)) {
                merged.add(id);
            }
        }
        return merged;
    }

    private List<Long> filterItemPoolByAllowedStatus(List<Long> itemIds, int limit) {
        if (itemIds == null || itemIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Auction> allowedAuctions = auctionRepository.findAllByItemIdInAndStatusIn(
                itemIds,
                new ArrayList<>(RECOMMENDATION_ALLOWED_STATUSES)
        );
        if (allowedAuctions.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> allowedItemIdSet = allowedAuctions.stream()
                .map(a -> a.getItem().getId())
                .collect(Collectors.toSet());

        List<Long> filtered = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Long itemId : itemIds) {
            if (allowedItemIdSet.contains(itemId) && seen.add(itemId)) {
                filtered.add(itemId);
                if (filtered.size() >= limit) {
                    break;
                }
            }
        }
        return filtered;
    }
    private List<AuctionRes> finalizeRecommendations(Long userId, Long contextItemId, Long contextAuctionId, List<Long> itemIds) {
        List<AuctionRes> base = convertItemIdsToAuctionRes(userId, itemIds, RECOMMENDATION_POOL_SIZE);
        List<AuctionRes> filtered = new ArrayList<>(filterOutContextAuction(base, contextAuctionId));

        Set<Long> excludeItemIds = filtered.stream()
                .map(AuctionRes::getItemId)
                .collect(Collectors.toSet());
        if (contextItemId != null) {
            excludeItemIds.add(contextItemId);
        }
        excludeItemIds.addAll(getUserViews(userId));

        if (filtered.size() < DEFAULT_RECOMMENDATION_LIMIT) {
            List<Long> extraItemIds = getTrendingItems(RECOMMENDATION_POOL_SIZE, excludeItemIds);
            if (!extraItemIds.isEmpty()) {
                List<AuctionRes> extraAuctions = convertItemIdsToAuctionRes(userId, extraItemIds, RECOMMENDATION_POOL_SIZE);
                Set<Long> existingAuctionIds = filtered.stream()
                        .map(AuctionRes::getAuctionId)
                        .collect(Collectors.toSet());
                for (AuctionRes extra : extraAuctions) {
                    if (contextAuctionId != null && contextAuctionId.equals(extra.getAuctionId())) {
                        continue;
                    }
                    if (existingAuctionIds.add(extra.getAuctionId())) {
                        filtered.add(extra);
                    }
                }
            }
        }

        Set<Long> excludeAuctionIds = filtered.stream()
                .map(AuctionRes::getAuctionId)
                .collect(Collectors.toSet());
        List<AuctionRes> newItems = getNewAuctions(userId, contextItemId, contextAuctionId, excludeAuctionIds);
        if (!newItems.isEmpty()) {
            for (AuctionRes fresh : newItems) {
                if (excludeAuctionIds.add(fresh.getAuctionId())) {
                    filtered.add(fresh);
                }
            }
        }

        if (filtered.size() <= DEFAULT_RECOMMENDATION_LIMIT) {
            return filtered;
        }

        List<AuctionRes> finalList = new ArrayList<>();
        if (!newItems.isEmpty()) {
            List<AuctionRes> newCandidates = new ArrayList<>(newItems);
            Collections.shuffle(newCandidates, RANDOM);
            int takeNew = Math.min(NEW_ITEMS_TARGET, newCandidates.size());
            finalList.addAll(newCandidates.subList(0, takeNew));
        }

        Set<Long> selectedIds = finalList.stream()
                .map(AuctionRes::getAuctionId)
                .collect(Collectors.toSet());
        List<AuctionRes> candidates = filtered.stream()
                .filter(a -> !selectedIds.contains(a.getAuctionId()))
                .collect(Collectors.toList());
        Collections.shuffle(candidates, RANDOM);

        int remaining = DEFAULT_RECOMMENDATION_LIMIT - finalList.size();
        if (remaining > 0) {
            finalList.addAll(candidates.subList(0, Math.min(remaining, candidates.size())));
        }
        return finalList;
    }

    private List<AuctionRes> getNewAuctions(Long userId, Long contextItemId, Long contextAuctionId, Set<Long> excludeAuctionIds) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(NEW_ITEMS_LOOKBACK_MINUTES);
        List<AuctionStatus> statuses = new ArrayList<>(RECOMMENDATION_ALLOWED_STATUSES);
        Pageable pageable = PageRequest.of(0, RECOMMENDATION_POOL_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Auction> auctions = auctionRepository.findNewAuctions(statuses, threshold, pageable);
        if (auctions == null || auctions.isEmpty()) {
            return new ArrayList<>();
        }
        List<Auction> filtered = new ArrayList<>();
        for (Auction auction : auctions) {
            if (contextAuctionId != null && contextAuctionId.equals(auction.getId())) {
                continue;
            }
            if (contextItemId != null && contextItemId.equals(auction.getItem().getId())) {
                continue;
            }
            if (excludeAuctionIds != null && excludeAuctionIds.contains(auction.getId())) {
                continue;
            }
            if (!RECOMMENDATION_ALLOWED_STATUSES.contains(auction.getStatus())) {
                continue;
            }
            filtered.add(auction);
        }
        if (filtered.isEmpty()) {
            return new ArrayList<>();
        }
        return toAuctionRes(userId, filtered);
    }

    private List<AuctionRes> toAuctionRes(Long userId, List<Auction> auctions) {
        if (auctions == null || auctions.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> itemIds = auctions.stream()
                .map(a -> a.getItem().getId())
                .collect(Collectors.toList());
        Set<Long> wishedItemIds = wishService.getWishedItemIds(userId, itemIds);
        return auctions.stream()
                .map(a -> AuctionRes.from(a, wishedItemIds.contains(a.getItem().getId())))
                .collect(Collectors.toCollection(ArrayList::new));
    }
    private List<AuctionRes> filterOutContextAuction(List<AuctionRes> auctions, Long contextAuctionId) {
        if (auctions == null || auctions.isEmpty() || contextAuctionId == null) {
            return auctions == null ? new ArrayList<>() : new ArrayList<>(auctions);
        }
        return auctions.stream()
                .filter(a -> !contextAuctionId.equals(a.getAuctionId()))
                .collect(Collectors.toCollection(ArrayList::new));
    }
    private record UserSimilarity(Long userId, double similarity) {}

    private List<AuctionRes> convertItemIdsToAuctionRes(Long userId, List<Long> itemIds, int limit) {
        if (itemIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> limitedItemIds = itemIds.stream()
                .limit(limit)
                .collect(Collectors.toList());

        List<Auction> recommendedAuctions = auctionRepository.findAllByItemIdIn(limitedItemIds);
        List<Auction> statusFilteredAuctions = recommendedAuctions.stream()
                .filter(auction -> RECOMMENDATION_ALLOWED_STATUSES.contains(auction.getStatus()))
                .collect(Collectors.toCollection(ArrayList::new));

        Set<Long> wishedItemIds = wishService.getWishedItemIds(userId, statusFilteredAuctions.stream()
                .map(a -> a.getItem().getId())
                .collect(Collectors.toList()));

        return statusFilteredAuctions.stream()
                .map(auction -> AuctionRes.from(auction, wishedItemIds.contains(auction.getItem().getId())))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
