package noonchissaum.backend.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClassifyCategoryReq {
    private ValueConfidence brand;
    private ValueConfidence model;
    @JsonProperty("text_ocr")
    private List<String> textOcr;
    private List<CategoryItem> categories;
    private List<String> keywords;
}
