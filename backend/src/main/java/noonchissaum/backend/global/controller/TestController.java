package noonchissaum.backend.global.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.global.dto.LocationDto;
import noonchissaum.backend.global.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/public/test")
@RequiredArgsConstructor
public class TestController {

    private final LocationService locationService;

    private static final Double GANGNAM_STATION_LAT = 37.497175;
    private static final Double GANGNAM_STATION_LNG = 127.027926;
    private static final Double YEOKSAM_STATION_LAT = 37.500622;
    private static final Double YEOKSAM_STATION_LNG = 127.036456;

    @GetMapping("/geocode")
    public ResponseEntity<LocationDto> testGeocode(@RequestParam String address) {
        LocationDto result = locationService.getCoordinates(address);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/distance")
    public ResponseEntity<Map<String, Object>> testDistance() {
        Double distance = locationService.calculateDistance(
                GANGNAM_STATION_LAT, GANGNAM_STATION_LNG,
                YEOKSAM_STATION_LAT, YEOKSAM_STATION_LNG
        );

        Map<String, Object> result = new HashMap<>();
        result.put("from", "강남역");
        result.put("to", "역삼역");
        result.put("distanceKm", Math.round(distance * 100) / 100.0);
        result.put("distanceM", Math.round(distance * 1000));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/within-radius")
    public ResponseEntity<Map<String, Object>> testWithinRadius(@RequestParam Double radiusKm) {
        Double distance = locationService.calculateDistance(
                GANGNAM_STATION_LAT, GANGNAM_STATION_LNG,
                YEOKSAM_STATION_LAT, YEOKSAM_STATION_LNG
        );

        boolean isWithin = distance <= radiusKm;

        Map<String, Object> result = new HashMap<>();
        result.put("기준", "강남역");
        result.put("대상", "역삼역");
        result.put("거리km", Math.round(distance * 100) / 100.0);
        result.put("검색반경km", radiusKm);
        result.put("범위내포함", isWithin);

        return ResponseEntity.ok(result);
    }
}