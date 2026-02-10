package noonchissaum.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class KakaoGeocodeRes {

    private Meta meta;
    private List<Document> documents;

    @Data
    @NoArgsConstructor
    public static class Meta {
        @JsonProperty("total_count")
        private Integer totalCount; //카카오api응답구조여서 사용하는부분

        @JsonProperty("pageable_count")
        private Integer pageableCount;

        @JsonProperty("is_end")
        private Boolean isEnd;
    }

    @Data
    @NoArgsConstructor
    public static class Document {
        @JsonProperty("address_name")
        private String addressName;

        @JsonProperty("address_type")
        private String addressType;

        private String x;  // 경도 (longitude)
        private String y;  // 위도 (latitude)

        private Address address;

        @JsonProperty("road_address")
        private RoadAddress roadAddress;
    }

    // 지번 주소

    @Data
    @NoArgsConstructor
    public static class Address {
        @JsonProperty("address_name")
        private String addressName;

        @JsonProperty("region_1depth_name")
        private String region1DepthName; // 시/도

        @JsonProperty("region_2depth_name")
        private String region2DepthName; // 구/군

        @JsonProperty("region_3depth_name")
        private String region3DepthName; // 동/읍/면

        @JsonProperty("mountain_yn")
        private String mountainYn; // N(일반), Y(산)

        @JsonProperty("main_address_no")
        private String mainAddressNo; // 지번 본번

        @JsonProperty("sub_address_no")
        private String subAddressNo; // 지번 부번
    }

    // 도로명 주소

    @Data
    @NoArgsConstructor
    public static class RoadAddress {
        @JsonProperty("address_name")
        private String addressName;

        @JsonProperty("region_1depth_name")
        private String region1DepthName; // 시/도

        @JsonProperty("region_2depth_name")
        private String region2DepthName; // 구/군

        @JsonProperty("region_3depth_name")
        private String region3DepthName; // 동/읍/면

        @JsonProperty("road_name")
        private String roadName;

        @JsonProperty("underground_yn")
        private String undergroundYn; // 지상 / 지하

        @JsonProperty("main_building_no")
        private String mainBuildingNo;

        @JsonProperty("sub_building_no")
        private String subBuildingNo;

        @JsonProperty("building_name")
        private String buildingName;

        @JsonProperty("zone_no")
        private String zoneNo;
    }
}
