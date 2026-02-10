package noonchissaum.backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchItemsRes {
    private Double requestedRadius;
    private Double searchedRadius;
    private Integer count;
    private List<LocationSearchResult> items;
    private String message;
}