package noonchissaum.backend.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ImageQuality {
    @JsonProperty("is_blurry")
    private boolean isBlurry;
    @JsonProperty("is_overexposed")
    private boolean isOverexposed;
    @JsonProperty("resolution_ok")
    private boolean resolutionOk;
}
