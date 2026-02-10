package noonchissaum.backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NaverAddress {
    private String roadAddress;
    private String jibunAddress;
    private String englishAddress;
    private String x;  // 경도
    private String y;  // 위도
    private Double distance;
}
