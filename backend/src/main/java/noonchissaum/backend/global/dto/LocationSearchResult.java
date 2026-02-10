package noonchissaum.backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationSearchResult {
    private Long itemId;
    private String title;
    private Double distance;
    private String sellerDong;
    private String reliability;
    private String reliabilityIcon;
}
