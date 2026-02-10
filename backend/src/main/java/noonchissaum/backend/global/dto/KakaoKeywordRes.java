package noonchissaum.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class KakaoKeywordRes {

    private Meta meta;
    private List<Document> documents;

    @Data
    @NoArgsConstructor
    public static class Meta { // 형식
        @JsonProperty("total_count")
        private Integer totalCount;

        @JsonProperty("pageable_count")
        private Integer pageableCount;

        @JsonProperty("is_end")
        private Boolean isEnd;
    }

    @Data
    @NoArgsConstructor
    public static class Document {
        @JsonProperty("place_name")
        private String placeName;//장소이름

        @JsonProperty("address_name")
        private String addressName;

        @JsonProperty("road_address_name")
        private String roadAddressName;

        private String x;  // 경도
        private String y;  // 위도
    }
}
