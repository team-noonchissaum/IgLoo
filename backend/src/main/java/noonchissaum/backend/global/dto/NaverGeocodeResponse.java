package noonchissaum.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NaverGeocodeResponse {
    private NaverGeocodeMeta meta;
    private List<NaverAddress> addresses;
}

