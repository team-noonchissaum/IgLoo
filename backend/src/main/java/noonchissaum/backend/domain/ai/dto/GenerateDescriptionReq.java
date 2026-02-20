package noonchissaum.backend.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateDescriptionReq {
    @JsonProperty("category_id")
    private Long categoryId;
    private String brand;
    private String model;
    private String condition;
    private List<Defect> defects;
    private List<String> accessories;
    @JsonProperty("image_urls")
    private List<String> imageUrls;
    @JsonProperty("item_type")
    private String itemType;
    @JsonProperty("start_price")
    private Integer startPrice;
    @JsonProperty("auction_duration")
    private Integer auctionDuration;
    @JsonProperty("start_at")
    private LocalDateTime startAt;
    @JsonProperty("end_at")
    private LocalDateTime endAt;
}
