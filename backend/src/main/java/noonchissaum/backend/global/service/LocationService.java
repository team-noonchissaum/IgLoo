package noonchissaum.backend.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.global.dto.LocationDto;
import noonchissaum.backend.global.dto.NaverAddress;
import noonchissaum.backend.global.dto.NaverGeocodeResponse;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final RestTemplate restTemplate;

    @Value("${naver.map.client-id}")
    private String clientId;

    @Value("${naver.map.client-secret}")
    private String clientSecret;

    @Value("${naver.map.geocode-url}")
    private String geocodeUrl;
    /**
     * 주소를 좌표(위도/경도)로 변환
     * @param address 변환할 도로명 주소
     * @return 좌표 정보 (위도, 경도)
     */
    public LocationDto getCoordinates(String address) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-NCP-APIGW-API-KEY-ID", clientId);
            headers.set("X-NCP-APIGW-API-KEY", clientSecret);

            // ⭐️ 반드시 필요
            headers.set("User-Agent", "Mozilla/5.0");

            //geocodeUrl사용
            String url = geocodeUrl
                    + "?query="
                    + URLEncoder.encode(address, StandardCharsets.UTF_8);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<NaverGeocodeResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            NaverGeocodeResponse.class
                    );
            //test
            log.info("NAVER GEOCODE STATUS = {}", response.getStatusCode());
            log.info("NAVER GEOCODE BODY = {}", response.getBody());

            log.error("NAVER CLIENT ID = [{}]", clientId);
            log.error("NAVER CLIENT SECRET = [{}]", clientSecret);
            log.error("NAVER GEOCODE URL = [{}]", geocodeUrl);

            // ⭐️ HTTP 상태 체크
//            if (!response.getStatusCode().is2xxSuccessful()) {
//                throw new ApiException(ErrorCode.LOCATION_API_ERROR);
//            }
//
//            NaverGeocodeResponse body = response.getBody();
//
//            // ⭐️ 네이버 status 체크
//            if (body == null
//                    || body.getAddresses() == null
//                    || body.getAddresses().isEmpty()) {
//                throw new ApiException(ErrorCode.ADDRESS_NOT_FOUND);
//            }
//
//            NaverAddress addr = response.getBody().getAddresses().get(0);
//
//            return LocationDto.builder()
//                    .latitude(Double.parseDouble(addr.getY()))
//                    .longitude(Double.parseDouble(addr.getX()))
//                    .address(addr.getRoadAddress())
//                    .jibunAddress(addr.getJibunAddress())
//                    .build();

            return null;
        }catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("NAVER API ERROR STATUS = {}", e.getStatusCode());
            log.error("NAVER API ERROR BODY = {}", e.getResponseBodyAsString());
            throw new ApiException(ErrorCode.LOCATION_API_ERROR);
        } catch (Exception e) {
            log.error("UNKNOWN ERROR", e);
            throw new ApiException(ErrorCode.LOCATION_API_ERROR);
        }
//        }  catch (ApiException e) {
//            // 이미 ApiException이면 그대로 던지기
//            throw e;
//        } catch (Exception e) {
//            throw new ApiException(ErrorCode.LOCATION_API_ERROR);
//        }
    }

    /**
     * 두 좌표 간의 거리 계산 (직선거리)
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
