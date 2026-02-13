package noonchissaum.backend.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCandidate {
    @JsonProperty("category_id")
    private Long categoryId;
    @JsonProperty("category_path")
    private String categoryPath;
    private Double confidence;
}
