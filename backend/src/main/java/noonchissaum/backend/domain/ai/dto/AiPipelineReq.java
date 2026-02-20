package noonchissaum.backend.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiPipelineReq {
    private List<String> imageUrls;
    private Map<String, Object> metadata;
    private Integer startPrice;
    private Integer auctionDuration;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
