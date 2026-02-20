package noonchissaum.backend.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.global.dto.KakaoGeocodeRes;
import noonchissaum.backend.global.dto.KakaoKeywordRes;
import noonchissaum.backend.global.dto.LocationDto;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final RestTemplate restTemplate;
    private final AuctionRepository auctionRepository;

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
        LocationDto result = null;
        boolean hadApiError = false;
        try {
            result = searchByAddress(address);
        } catch (ApiException e) {
            if (e.getErrorCode() == ErrorCode.LOCATION_API_ERROR
                    || e.getErrorCode() == ErrorCode.LOCATION_ENCODING_ERROR) {
                hadApiError = true;
            } else {
                throw e;
            }
        }

        // 2. 실패시 키워드 검색으로 fallback
        if (result == null) {
            log.info("주소 검색 실패, 키워드 검색으로 fallback");
            try {
                result = searchByKeyword(address);
            } catch (ApiException e) {
                if (e.getErrorCode() == ErrorCode.LOCATION_API_ERROR
                        || e.getErrorCode() == ErrorCode.LOCATION_ENCODING_ERROR) {
                    hadApiError = true;
                } else {
                    throw e;
                }
            }
        }

        // 3. 둘 다 실패
        if (result == null) {
            if (hadApiError) {
                throw new ApiException(ErrorCode.LOCATION_API_ERROR);
            }
            throw new ApiException(ErrorCode.ADDRESS_NOT_FOUND);
        }

        return result;
    }

    /**
     * 주소 검색 API (도로명/지번 주소)->입력한 주소를 좌표로 변환
     */
    private LocationDto searchByAddress(String address) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);

            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String urlString = KAKAO_ADDRESS_URL + "?query=" + encodedAddress;

//            log.info("=== 주소 검색 URL: {}", urlString);

            // URI 객체로 변환해서 이중 인코딩 방지
            URI uri = new URI(urlString);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<KakaoGeocodeRes> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    KakaoGeocodeRes.class
            );

//            log.info("주소 검색 응답: {}", response.getBody());

            KakaoGeocodeRes body = response.getBody();
            if (body == null || body.getDocuments() == null || body.getDocuments().isEmpty()) {
                return null;
            }

            KakaoGeocodeRes.Document doc = body.getDocuments().get(0);
            // ← 로그 추가
            log.info("=== 카카오 API 응답 ===");
            log.info("입력 주소: {}", address);
            log.info("도로명 주소: {}", doc.getRoadAddress());
            log.info("지번 주소: {}", doc.getAddress());

            String jibunAddress = doc.getAddress() != null
                    ? doc.getAddress().getAddressName() : null;
            log.info("지번 주소명: {}", jibunAddress);

            String dongFromJibun = extractDongFromAddress(jibunAddress);
            log.info("추출된 동: {}", dongFromJibun);

            String roadAddress = doc.getRoadAddress() != null
                    ? doc.getRoadAddress().getAddressName() : null;

//            String jibunAddress = doc.getAddress() != null
//                    ? doc.getAddress().getAddressName() : null;
//            String dongFromJibun = extractDongFromAddress(jibunAddress);


            return LocationDto.builder()
                    .latitude(Double.parseDouble(doc.getY()))
                    .longitude(Double.parseDouble(doc.getX()))
                    .address(roadAddress != null ? roadAddress : doc.getAddressName())
                    .jibunAddress(jibunAddress)
                    .dong(dongFromJibun)  // ← 지번에서 추출한 동
                    .build();

        } catch (Exception e) {
            log.warn("주소 검색 실패: {}", e.getMessage());
            throw new ApiException(ErrorCode.LOCATION_API_ERROR);
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
            String jibunAddress = doc.getAddressName();  //도로명 주소 가져오면
            String dongFromJibun = extractDongFromAddress(jibunAddress);//거기서 dong을 추출함

            return LocationDto.builder()
                    .latitude(doc.getY())
                    .longitude(doc.getX())
                    .address(doc.getRoadAddressName() != null ? doc.getRoadAddressName() : doc.getAddressName())
                    .jibunAddress(doc.getAddressName())
                    .dong(dongFromJibun)
                    .build();

        } catch (Exception e) {
            log.error("키워드 검색 실패: {}", e.getMessage());
            throw new ApiException(ErrorCode.LOCATION_API_ERROR);
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

    /**
     * KM를 M로전환->ST_Distance_Sphere() 함수계산용
     */
    public Double convertKmToMeters(Double radiusKm) {
        if (radiusKm == null || radiusKm <= 0) {
            throw new ApiException(ErrorCode.INVALID_RADIUS);
        }
        return radiusKm * 1000;
    }

    private String extractDongFromAddress(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }

        String[] parts = address.split(" ");

        // "동(洞)"으로 끝나는 부분 찾기
        for (String part : parts) {
            if (part.endsWith("동")) {
                return part;
            }
        }

        // "동"이 없으면 "구(區)" 찾기 (fallback)
        for (String part : parts) {
            if (part.endsWith("구")) {
                return part;
            }
        }

        // 둘 다 없으면 마지막 부분 반환
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }

    /**
     * 키워드로 주소 검색 (자동완성용)
     */
    public List<KakaoKeywordRes.Document> searchAddressByKeyword(String keyword) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);

            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String urlString = KAKAO_KEYWORD_URL + "?query=" + encodedKeyword
                    + "&size=10";  // 최대 10개

            URI uri = new URI(urlString);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<KakaoKeywordRes> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    KakaoKeywordRes.class
            );

            KakaoKeywordRes body = response.getBody();
            if (body == null || body.getDocuments() == null) {
                return List.of();
            }

            return body.getDocuments();

        } catch (Exception e) {
            log.error("키워드 검색 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /**키워드  기반 자동완성 주소검색 제공->정확한 위치 가져오기*/
    public List<LocationDto> searchAddress(String keyword) {
        if(keyword==null || keyword.isEmpty()){
            throw new ApiException(ErrorCode.INVALID_ADDRESS);
        }
        try {
            List<KakaoKeywordRes.Document> documents =
                    this.searchAddressByKeyword(keyword);

            if (documents == null || documents.isEmpty()) {
                return List.of();
            }

            Set<String> uniqueAddresses = new LinkedHashSet<>();
            List<LocationDto> suggestions = new ArrayList<>();

            for (KakaoKeywordRes.Document doc : documents) {
                String address = doc.getAddressName();

                //검색어가 결과에 포함되었는지 확인->이게없으면 카카오 api특성상 키워드와 유사한장소를 찾음(키워드포함 혹은 관련장소)
                if(!isValidAddress(keyword, address)) {
                    log.warn("검색 결과 부정확 - 검색어: {}, 결과: {}", keyword, address);
                    continue;
                }

                if (uniqueAddresses.contains(address)) {
                    continue;
                }
                uniqueAddresses.add(address);

                String roadAddress = doc.getRoadAddressName();
                if (roadAddress == null || roadAddress.isBlank()) {
                    roadAddress = null;
                }
                suggestions.add(LocationDto.builder()
                        .address(roadAddress != null ? roadAddress : address)
                        .jibunAddress(address)
                        .latitude(doc.getY())
                        .longitude(doc.getX())
                        .dong(null)  // ← 아직 계산 안 함
                        .build());

                if (suggestions.size() >= 10) {
                    break;
                }
            }

            return suggestions;

        } catch (Exception e) {
            log.error("주소 검색 실패: {}", e.getMessage());
            return List.of();
        }
    }
    /**
     * 입력한 주소와 검색 결과가 유사한지 확인
     */
    private boolean isValidAddress(String searchKeyword, String address) {
        if (searchKeyword == null || searchKeyword.isBlank() ||
                address == null || address.isBlank()) {
            return false;
        }

        String[] inputKeywords = searchKeyword.split(" ");

        for (String keyword : inputKeywords) {
            if (keyword.length() >= 2) {
                if (!address.contains(keyword)) {
                    log.warn("주소 검증 실패 - 입력: {}, 결과: {}", searchKeyword, address);
                    return false;
                }
            }
        }
        return true;
    }
}



