package noonchissaum.backend.domain.ai.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.ai.dto.AiPipelineReq;
import noonchissaum.backend.domain.ai.dto.AiPipelineRes;
import noonchissaum.backend.domain.ai.dto.AnalyzeImageReq;
import noonchissaum.backend.domain.ai.dto.AnalyzeImageRes;
import noonchissaum.backend.domain.ai.dto.CategoryCandidate;
import noonchissaum.backend.domain.ai.dto.CategoryItem;
import noonchissaum.backend.domain.ai.dto.ClassifyCategoryReq;
import noonchissaum.backend.domain.ai.dto.ClassifyCategoryRes;
import noonchissaum.backend.domain.ai.dto.GenerateDescriptionReq;
import noonchissaum.backend.domain.ai.dto.GenerateDescriptionRes;
import noonchissaum.backend.domain.ai.dto.ValueConfidence;
import noonchissaum.backend.domain.category.dto.res.CategoryListRes;
import noonchissaum.backend.domain.category.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiServiceClient client;
    private final CategoryService categoryService;
    private final CategoryAliasProvider categoryAliasProvider;

    public AnalyzeImageRes analyzeImage(AnalyzeImageReq request) {
        return client.analyzeImage(request);
    }

    public ClassifyCategoryRes classifyCategory(ClassifyCategoryReq request) {
        return client.classifyCategory(request);
    }

    public GenerateDescriptionRes generateDescription(GenerateDescriptionReq request) {
        return client.generateDescription(request);
    }

    public AiPipelineRes runPipeline(AiPipelineReq request) {
        AnalyzeImageRes analyze = client.analyzeImage(
                new AnalyzeImageReq(request.getImageUrls(), request.getMetadata())
        );

        List<CategoryListRes> categoryList = categoryService.categoryList();
        Map<Long, List<String>> aliases = categoryAliasProvider.getAliases();
        List<CategoryItem> categories = buildCategoryItems(categoryList, aliases);
        String keywordText = buildKeywordText(request.getMetadata(), analyze);
        List<String> keywordList = buildKeywordList(request.getMetadata(), analyze);

        ClassifyCategoryRes classify = client.classifyCategory(
                new ClassifyCategoryReq(
                        analyze.getBrand(),
                        analyze.getModel(),
                        analyze.getTextOcr(),
                        categories,
                        keywordList
                )
        );
        ClassifyCategoryRes normalizedClassify = normalizeCategorySelection(
                applyAliasMapping(classify, categories, keywordText),
                categories
        );
        normalizedClassify = enforceLeafSelection(normalizedClassify, categories);
        normalizedClassify = enrichCandidates(normalizedClassify, categories);

        GenerateDescriptionReq generateReq = new GenerateDescriptionReq(
                normalizedClassify.getSelectedCategoryId(),
                valueOrNull(analyze.getBrand()),
                valueOrNull(analyze.getModel()),
                valueOrNull(analyze.getCondition()),
                listOrEmpty(analyze.getDefects()),
                listOrEmpty(analyze.getAccessories()),
                request.getImageUrls(),
                findCategoryName(categories, normalizedClassify.getSelectedCategoryId()),
                request.getStartPrice(),
                request.getAuctionDuration(),
                request.getStartAt(),
                request.getEndAt()
        );

        GenerateDescriptionRes description = client.generateDescription(generateReq);

        return new AiPipelineRes(analyze, normalizedClassify, description);
    }

    private List<CategoryItem> buildCategoryItems(
            List<CategoryListRes> categories,
            Map<Long, List<String>> aliases
    ) {
        Map<Long, CategoryListRes> byId = new HashMap<>();
        Map<Long, List<CategoryListRes>> childrenMap = new HashMap<>();
        for (CategoryListRes res : categories) {
            byId.put(res.getId(), res);
            if (res.getParentId() != null) {
                childrenMap.computeIfAbsent(res.getParentId(), key -> new ArrayList<>())
                        .add(res);
            }
        }
        Map<Long, String> pathCache = new HashMap<>();
        return categories.stream()
                .map(res -> new CategoryItem(
                        res.getId(),
                        res.getName(),
                        res.getParentId(),
                        buildCategoryPath(res, byId, pathCache),
                        aliases.getOrDefault(res.getId(), List.of()),
                        isLeaf(res.getId(), childrenMap)
                ))
                .toList();
    }

    private boolean isLeaf(Long categoryId, Map<Long, List<CategoryListRes>> childrenMap) {
        if (categoryId == null) {
            return false;
        }
        List<CategoryListRes> children = childrenMap.get(categoryId);
        return children == null || children.isEmpty();
    }

    private String buildCategoryPath(
            CategoryListRes category,
            Map<Long, CategoryListRes> byId,
            Map<Long, String> cache
    ) {
        if (category == null) {
            return "";
        }
        Long id = category.getId();
        String cached = cache.get(id);
        if (cached != null) {
            return cached;
        }
        Long parentId = category.getParentId();
        String path;
        if (parentId == null) {
            path = category.getName();
        } else {
            CategoryListRes parent = byId.get(parentId);
            String parentPath = parent == null ? "" : buildCategoryPath(parent, byId, cache);
            path = parentPath.isBlank() ? category.getName() : parentPath + " > " + category.getName();
        }
        cache.put(id, path);
        return path;
    }

    private ClassifyCategoryRes normalizeCategorySelection(
            ClassifyCategoryRes classify,
            List<CategoryItem> categories
    ) {
        if (categories == null || categories.isEmpty()) {
            return classify;
        }
        Set<Long> ids = categories.stream()
                .map(CategoryItem::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        Long selectedId = classify != null ? classify.getSelectedCategoryId() : null;
        boolean needsFallback = selectedId == null || !ids.contains(selectedId);
        if (!needsFallback) {
            return classify;
        }

        Long fallbackId = findMiscCategoryId(categories)
                .orElse(categories.get(0).getId());
        String fallbackPath = findCategoryPath(categories, fallbackId).orElse("기타");

        List<CategoryCandidate> candidates = new ArrayList<>(
                classify != null && classify.getCategoryCandidates() != null
                        ? classify.getCategoryCandidates()
                        : List.of()
        );
        boolean alreadyExists = candidates.stream()
                .anyMatch(c -> Objects.equals(c.getCategoryId(), fallbackId));
        if (!alreadyExists && fallbackId != null) {
            candidates.add(new CategoryCandidate(fallbackId, fallbackPath, 0.01));
        }

        return new ClassifyCategoryRes(
                candidates,
                fallbackId,
                "fallback to 기타 category",
                true
        );
    }

    private ClassifyCategoryRes enforceLeafSelection(
            ClassifyCategoryRes classify,
            List<CategoryItem> categories
    ) {
        if (classify == null || categories == null || categories.isEmpty()) {
            return classify;
        }
        Long selectedId = classify.getSelectedCategoryId();
        if (selectedId == null) {
            return classify;
        }
        CategoryItem selected = findCategoryItem(categories, selectedId);
        if (selected != null && selected.isLeaf()) {
            return classify;
        }

        CategoryCandidate leafCandidate = pickBestLeafCandidate(classify, categories);
        if (leafCandidate != null) {
            return new ClassifyCategoryRes(
                    classify.getCategoryCandidates(),
                    leafCandidate.getCategoryId(),
                    "leaf candidate selected",
                    classify.isNeedsUserConfirmation()
            );
        }

        Long childLeafId = findFirstLeafChildId(categories, selectedId);
        if (childLeafId != null) {
            return new ClassifyCategoryRes(
                    classify.getCategoryCandidates(),
                    childLeafId,
                    "leaf child selected",
                    true
            );
        }

        Long fallbackId = findMiscCategoryId(categories)
                .orElse(selectedId);
        return new ClassifyCategoryRes(
                classify.getCategoryCandidates(),
                fallbackId,
                "leaf fallback to 기타 category",
                true
        );
    }

    private ClassifyCategoryRes enrichCandidates(
            ClassifyCategoryRes classify,
            List<CategoryItem> categories
    ) {
        if (classify == null || categories == null || categories.isEmpty()) {
            return classify;
        }
        Long selectedId = classify.getSelectedCategoryId();
        if (selectedId == null) {
            return classify;
        }
        List<CategoryCandidate> candidates = new ArrayList<>(
                classify.getCategoryCandidates() != null
                        ? classify.getCategoryCandidates()
                        : List.of()
        );
        boolean exists = candidates.stream()
                .anyMatch(c -> Objects.equals(c.getCategoryId(), selectedId));
        if (!exists) {
            String path = findCategoryPath(categories, selectedId).orElse("unknown");
            candidates.add(new CategoryCandidate(selectedId, path, 0.6));
        }
        return new ClassifyCategoryRes(
                candidates,
                selectedId,
                classify.getSelectionReason(),
                classify.isNeedsUserConfirmation()
        );
    }

    private CategoryItem findCategoryItem(List<CategoryItem> categories, Long id) {
        if (id == null) {
            return null;
        }
        return categories.stream()
                .filter(c -> Objects.equals(c.getId(), id))
                .findFirst()
                .orElse(null);
    }

    private CategoryCandidate pickBestLeafCandidate(
            ClassifyCategoryRes classify,
            List<CategoryItem> categories
    ) {
        if (classify.getCategoryCandidates() == null || classify.getCategoryCandidates().isEmpty()) {
            return null;
        }
        return classify.getCategoryCandidates().stream()
                .filter(c -> isLeafCategory(categories, c.getCategoryId()))
                .max((a, b) -> Double.compare(
                        a.getConfidence() != null ? a.getConfidence() : 0.0,
                        b.getConfidence() != null ? b.getConfidence() : 0.0
                ))
                .orElse(null);
    }

    private boolean isLeafCategory(List<CategoryItem> categories, Long id) {
        CategoryItem item = findCategoryItem(categories, id);
        return item != null && item.isLeaf();
    }

    private Long findFirstLeafChildId(List<CategoryItem> categories, Long parentId) {
        if (parentId == null) {
            return null;
        }
        return categories.stream()
                .filter(c -> Objects.equals(c.getParentId(), parentId))
                .filter(CategoryItem::isLeaf)
                .map(CategoryItem::getId)
                .findFirst()
                .orElse(null);
    }

    private ClassifyCategoryRes applyAliasMapping(
            ClassifyCategoryRes classify,
            List<CategoryItem> categories,
            String keywordText
    ) {
        if (keywordText == null || keywordText.isBlank() || categories == null || categories.isEmpty()) {
            return classify;
        }
        AliasMatch bestMatch = findBestAliasMatch(categories, keywordText);
        if (bestMatch == null || bestMatch.score <= 0) {
            return classify;
        }
        if (!shouldOverrideByAlias(classify, bestMatch)) {
            return classify;
        }

        String path = findCategoryPath(categories, bestMatch.categoryId).orElse("alias match");
        List<CategoryCandidate> candidates = new ArrayList<>(
                classify != null && classify.getCategoryCandidates() != null
                        ? classify.getCategoryCandidates()
                        : List.of()
        );
        boolean alreadyExists = candidates.stream()
                .anyMatch(c -> Objects.equals(c.getCategoryId(), bestMatch.categoryId));
        if (!alreadyExists) {
            candidates.add(new CategoryCandidate(bestMatch.categoryId, path, 0.9));
        }
        return new ClassifyCategoryRes(
                candidates,
                bestMatch.categoryId,
                "alias match override",
                false
        );
    }

    private boolean shouldOverrideByAlias(ClassifyCategoryRes classify, AliasMatch match) {
        if (match != null && match.score >= 1) {
            return true;
        }
        if (classify == null || classify.getSelectedCategoryId() == null) {
            return true;
        }
        Double topConfidence = classify.getCategoryCandidates() == null
                ? null
                : classify.getCategoryCandidates().stream()
                .map(CategoryCandidate::getConfidence)
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElse(null);
        return topConfidence == null || topConfidence < 0.45;
    }

    private AliasMatch findBestAliasMatch(List<CategoryItem> categories, String keywordText) {
        String normalized = normalizeText(keywordText);
        AliasMatch best = null;
        for (CategoryItem category : categories) {
            if (category.getAliases() == null || category.getAliases().isEmpty()) {
                continue;
            }
            int score = 0;
            for (String alias : category.getAliases()) {
                String normAlias = normalizeText(alias);
                if (!normAlias.isBlank() && normalized.contains(normAlias)) {
                    score++;
                }
            }
            if (score > 0 && (best == null || score > best.score)) {
                best = new AliasMatch(category.getId(), score);
            }
        }
        return best;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "").toLowerCase();
    }

    private String buildKeywordText(Map<String, Object> metadata, AnalyzeImageRes analyze) {
        StringBuilder builder = new StringBuilder();
        if (metadata != null) {
            for (Object value : metadata.values()) {
                if (value != null) {
                    builder.append(' ').append(value);
                }
            }
        }
        if (analyze != null) {
            if (analyze.getTextOcr() != null) {
                for (String text : analyze.getTextOcr()) {
                    if (text != null) {
                        builder.append(' ').append(text);
                    }
                }
            }
            if (analyze.getBrand() != null && analyze.getBrand().getValue() != null) {
                builder.append(' ').append(analyze.getBrand().getValue());
            }
            if (analyze.getModel() != null && analyze.getModel().getValue() != null) {
                builder.append(' ').append(analyze.getModel().getValue());
            }
        }
        return builder.toString();
    }

    private List<String> buildKeywordList(Map<String, Object> metadata, AnalyzeImageRes analyze) {
        List<String> keywords = new ArrayList<>();
        if (metadata != null) {
            for (Object value : metadata.values()) {
                if (value != null) {
                    addTokens(keywords, value.toString());
                }
            }
        }
        if (analyze != null) {
            if (analyze.getTextOcr() != null) {
                for (String text : analyze.getTextOcr()) {
                    addTokens(keywords, text);
                }
            }
            if (analyze.getBrand() != null && analyze.getBrand().getValue() != null) {
                addTokens(keywords, analyze.getBrand().getValue());
            }
            if (analyze.getModel() != null && analyze.getModel().getValue() != null) {
                addTokens(keywords, analyze.getModel().getValue());
            }
        }
        return keywords;
    }

    private void addTokens(List<String> keywords, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String token : text.split("\\s+")) {
            String trimmed = token.trim();
            if (!trimmed.isBlank()) {
                keywords.add(trimmed);
            }
        }
    }

    private static class AliasMatch {
        private final Long categoryId;
        private final int score;

        private AliasMatch(Long categoryId, int score) {
            this.categoryId = categoryId;
            this.score = score;
        }
    }

    private Optional<Long> findMiscCategoryId(List<CategoryItem> categories) {
        return categories.stream()
                .filter(c -> c.getName() != null)
                .filter(c -> c.getName().trim().equals("기타물품") && c.isLeaf())
                .map(CategoryItem::getId)
                .findFirst()
                .or(() -> categories.stream()
                        .filter(c -> c.getName() != null)
                        .filter(c -> c.getName().trim().equals("기타") && c.isLeaf())
                        .map(CategoryItem::getId)
                        .findFirst());
    }

    private Optional<String> findCategoryPath(List<CategoryItem> categories, Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return categories.stream()
                .filter(c -> Objects.equals(c.getId(), id))
                .map(CategoryItem::getPath)
                .filter(path -> path != null && !path.isBlank())
                .findFirst();
    }

    private String findCategoryName(List<CategoryItem> categories, Long id) {
        if (id == null) {
            return null;
        }
        return categories.stream()
                .filter(c -> Objects.equals(c.getId(), id))
                .map(CategoryItem::getName)
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String valueOrNull(ValueConfidence confidence) {
        return confidence == null ? null : confidence.getValue();
    }

    private <T> List<T> listOrEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }
}
