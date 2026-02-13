package noonchissaum.backend.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeImageRes {
    private ValueConfidence brand;
    private ValueConfidence model;
    private ValueConfidence condition;
    private List<Defect> defects;
    private List<String> accessories;
    @JsonProperty("text_ocr")
    private List<String> textOcr;
    @JsonProperty("image_quality")
    private ImageQuality imageQuality;
}
