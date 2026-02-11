package noonchissaum.backend.domain.ai.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.ai.AiServiceProperties;
import noonchissaum.backend.domain.ai.dto.AnalyzeImageReq;
import noonchissaum.backend.domain.ai.dto.AnalyzeImageRes;
import noonchissaum.backend.domain.ai.dto.ClassifyCategoryReq;
import noonchissaum.backend.domain.ai.dto.ClassifyCategoryRes;
import noonchissaum.backend.domain.ai.dto.GenerateDescriptionReq;
import noonchissaum.backend.domain.ai.dto.GenerateDescriptionRes;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class AiServiceClient {

    private final AiServiceProperties properties;
    private final RestTemplate restTemplate;

    public AnalyzeImageRes analyzeImage(AnalyzeImageReq request) {
        return restTemplate.exchange(
                properties.getBaseUrl() + "/ai/analyze-image",
                HttpMethod.POST,
                new HttpEntity<>(request, defaultHeaders()),
                AnalyzeImageRes.class
        ).getBody();
    }

    public ClassifyCategoryRes classifyCategory(ClassifyCategoryReq request) {
        return restTemplate.exchange(
                properties.getBaseUrl() + "/ai/classify-category",
                HttpMethod.POST,
                new HttpEntity<>(request, defaultHeaders()),
                ClassifyCategoryRes.class
        ).getBody();
    }

    public GenerateDescriptionRes generateDescription(GenerateDescriptionReq request) {
        return restTemplate.exchange(
                properties.getBaseUrl() + "/ai/generate-description",
                HttpMethod.POST,
                new HttpEntity<>(request, defaultHeaders()),
                GenerateDescriptionRes.class
        ).getBody();
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
