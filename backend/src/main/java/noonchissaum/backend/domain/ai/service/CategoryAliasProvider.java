package noonchissaum.backend.domain.ai.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CategoryAliasProvider {

    private static final String RESOURCE_PATH = "classpath:category-aliases.json";

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    private Map<Long, List<String>> cachedAliases;

    public Map<Long, List<String>> getAliases() {
        if (cachedAliases != null) {
            return cachedAliases;
        }
        cachedAliases = loadAliases();
        return cachedAliases;
    }

    private Map<Long, List<String>> loadAliases() {
        Resource resource = resourceLoader.getResource(RESOURCE_PATH);
        if (!resource.exists()) {
            return Collections.emptyMap();
        }
        try (InputStream inputStream = resource.getInputStream()) {
            CategoryAliasesFile file = objectMapper.readValue(inputStream, CategoryAliasesFile.class);
            if (file.aliases == null) {
                return Collections.emptyMap();
            }
            Map<Long, List<String>> map = new HashMap<>();
            for (CategoryAliasEntry entry : file.aliases) {
                if (entry.categoryId != null && entry.aliases != null) {
                    map.put(entry.categoryId, entry.aliases);
                }
            }
            return map;
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CategoryAliasesFile {
        @JsonProperty("aliases")
        private List<CategoryAliasEntry> aliases;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CategoryAliasEntry {
        @JsonProperty("categoryId")
        private Long categoryId;
        @JsonProperty("aliases")
        private List<String> aliases;
    }
}
