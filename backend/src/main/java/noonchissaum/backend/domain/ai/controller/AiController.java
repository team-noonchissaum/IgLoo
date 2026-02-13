package noonchissaum.backend.domain.ai.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.ai.dto.AiPipelineReq;
import noonchissaum.backend.domain.ai.dto.AiPipelineRes;
import noonchissaum.backend.domain.ai.dto.AnalyzeImageReq;
import noonchissaum.backend.domain.ai.dto.AnalyzeImageRes;
import noonchissaum.backend.domain.ai.dto.ClassifyCategoryReq;
import noonchissaum.backend.domain.ai.dto.ClassifyCategoryRes;
import noonchissaum.backend.domain.ai.dto.GenerateDescriptionReq;
import noonchissaum.backend.domain.ai.dto.GenerateDescriptionRes;
import noonchissaum.backend.domain.ai.service.AiService;
import noonchissaum.backend.global.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/analyze-image")
    public ResponseEntity<ApiResponse<AnalyzeImageRes>> analyzeImage(@RequestBody AnalyzeImageReq request) {
        AnalyzeImageRes result = aiService.analyzeImage(request);
        return ResponseEntity.ok(ApiResponse.success("AI image analysis completed", result));
    }

    @PostMapping("/classify-category")
    public ResponseEntity<ApiResponse<ClassifyCategoryRes>> classifyCategory(@RequestBody ClassifyCategoryReq request) {
        ClassifyCategoryRes result = aiService.classifyCategory(request);
        return ResponseEntity.ok(ApiResponse.success("AI category classification completed", result));
    }

    @PostMapping("/generate-description")
    public ResponseEntity<ApiResponse<GenerateDescriptionRes>> generateDescription(@RequestBody GenerateDescriptionReq request) {
        GenerateDescriptionRes result = aiService.generateDescription(request);
        return ResponseEntity.ok(ApiResponse.success("AI description generated", result));
    }

    @PostMapping("/pipeline")
    public ResponseEntity<ApiResponse<AiPipelineRes>> runPipeline(@RequestBody AiPipelineReq request) {
        AiPipelineRes result = aiService.runPipeline(request);
        return ResponseEntity.ok(ApiResponse.success("AI pipeline completed", result));
    }
}
