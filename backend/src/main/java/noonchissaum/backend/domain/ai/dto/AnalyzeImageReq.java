package noonchissaum.backend.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeImageReq {
    private List<String> imageUrls;
    private Map<String, Object> metadata;
}
