package noonchissaum.backend.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiPipelineRes {
    private AnalyzeImageRes analyzeResult;
    private ClassifyCategoryRes classifyResult;
    private GenerateDescriptionRes descriptionResult;
}
