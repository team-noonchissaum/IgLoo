package noonchissaum.backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NaverGeocodeMeta {
    private Integer totalCount;
    private Integer page;
    private Integer count;
}
