package noonchissaum.backend.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.global.dto.KakaoGeocodeRes;
import noonchissaum.backend.global.dto.KakaoKeywordRes;
import noonchissaum.backend.global.dto.LocationDto;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final RestTemplate restTemplate;

    @Value("${kakao.map.rest-api-key}")
    private String kakaoRestApiKey;

    private static final String KAKAO_ADDRESS_URL = "https://dapi.kakao.com/v2/local/search/address.json";
    private static final String KAKAO_KEYWORD_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

    /**
     * 주소를 좌표(위도/경도)로 변환 - 카카오 API 사용
     * 주소 검색 실패시 키워드 검색으로 fallback
     */
    public LocationDto getCoordinates(String address) {
        log.info("=== 좌표 변환 시작 === address: {}", address);

        // 1. 주소 검색 시도
        LocationDto result = searchByAddress(address);

        // 2. 실패시 키워드 검색으로 fallback
        if (result == null) {
            log.info("주소 검색 실패, 키워드 검색으로 fallback");
            result = searchByKeyword(address);
        }

        // 3. 둘 다 실패
        if (result == null) {
            throw new ApiException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        return result;
    }

    /**
     * 주소 검색 API (도로명/지번 주소)
     */
    private LocationDto searchByAddress(String address) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);

            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String urlString = KAKAO_ADDRESS_URL + "?query=" + encodedAddress;

            log.info("=== 주소 검색 URL: {}", urlString);

            // URI 객체로 변환해서 이중 인코딩 방지
            URI uri = new URI(urlString);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<KakaoGeocodeRes> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    KakaoGeocodeRes.class
            );

            log.info("주소 검색 응답: {}", response.getBody());

            KakaoGeocodeRes body = response.getBody();
            if (body == null || body.getDocuments() == null || body.getDocuments().isEmpty()) {
                return null;
            }

            KakaoGeocodeRes.Document doc = body.getDocuments().get(0);

            String roadAddress = doc.getRoadAddress() != null
                    ? doc.getRoadAddress().getAddressName() : null;
            String jibunAddress = doc.getAddress() != null
                    ? doc.getAddress().getAddressName() : null;

            return LocationDto.builder()
                    .latitude(Double.parseDouble(doc.getY()))
                    .longitude(Double.parseDouble(doc.getX()))
                    .address(roadAddress != null ? roadAddress : doc.getAddressName())
                    .jibunAddress(jibunAddress != null ? jibunAddress : doc.getAddressName())
                    .build();

        } catch (Exception e) {
            log.warn("주소 검색 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 키워드 검색 API (동 이름, 장소명 등)
     */
    private LocationDto searchByKeyword(String keyword) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);

            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String urlString = KAKAO_KEYWORD_URL + "?query=" + encodedKeyword;

            log.info("=== 키워드 검색 URL: {}", urlString);

            // URI 객체로 변환해서 이중 인코딩 방지
            URI uri = new URI(urlString);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<KakaoKeywordRes> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    KakaoKeywordRes.class
            );

            log.info("키워드 검색 응답: {}", response.getBody());

            KakaoKeywordRes body = response.getBody();
            if (body == null || body.getDocuments() == null || body.getDocuments().isEmpty()) {
                return null;
            }

            KakaoKeywordRes.Document doc = body.getDocuments().get(0);

            return LocationDto.builder()
                    .latitude(Double.parseDouble(doc.getY()))
                    .longitude(Double.parseDouble(doc.getX()))
                    .address(doc.getRoadAddressName() != null ? doc.getRoadAddressName() : doc.getAddressName())
                    .jibunAddress(doc.getAddressName())
                    .build();

        } catch (Exception e) {
            log.error("키워드 검색 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 두 좌표 간의 거리 계산 (직선거리, km 단위)
     */
    public Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        final int EARTH_RADIUS = 6371;

        Double dLat = Math.toRadians(lat2 - lat1);
        Double dLon = Math.toRadians(lon2 - lon1);

        Double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
}
