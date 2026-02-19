package noonchissaum.backend.domain.location;


import noonchissaum.backend.global.dto.KakaoKeywordRes;
import noonchissaum.backend.global.dto.LocationDto;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationService테스트")
public class LocationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private LocationService locationService;

    private KakaoKeywordRes.Document mockDocument;

    @BeforeEach
    public void setup() {
        mockDocument = new KakaoKeywordRes.Document();
        mockDocument.setPlaceName("강남역");
        mockDocument.setAddressName("서울 강남구 강남동 123");
        mockDocument.setRoadAddressName("서울 강남구 테헤란로 152");
        mockDocument.setX(127.0276);
        mockDocument.setY(37.4979);
    }
    @Test
    @DisplayName("키워드검색 성공")
    void testSearchAddressByKeyword() {
        String word="강남";
        KakaoKeywordRes mockRes = new KakaoKeywordRes();
        mockRes.setDocuments(List.of(mockDocument));

        when(restTemplate.exchange(
                any(URI.class),
                any(),
                any(HttpEntity.class),
                eq(KakaoKeywordRes.class)
        )).thenReturn(new ResponseEntity<>(mockRes, HttpStatus.OK));

        List<KakaoKeywordRes.Document> result = locationService.searchAddressByKeyword(word);
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAddressName()).isEqualTo("서울 강남구 강남동 123");
        assertThat(result.get(0).getRoadAddressName()).isEqualTo("서울 강남구 테헤란로 152");
    }

    @Test
    @DisplayName("키워드 검색 실패 - 결과 없음")
    void testSearchAddressByKeyword_NoResult() {
        // Given
        String keyword = "시울";
        KakaoKeywordRes mockResponse = new KakaoKeywordRes();
        mockResponse.setDocuments(new ArrayList<>());

        when(restTemplate.exchange(
                any(URI.class),
                any(),
                any(HttpEntity.class),
                eq(KakaoKeywordRes.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // When
        List<KakaoKeywordRes.Document> result = locationService.searchAddressByKeyword(keyword);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("자동완성 주소 검색 성공")
    void testSearchAddress_Success() {
        // Given
        String keyword = "강남";
        KakaoKeywordRes mockResponse = new KakaoKeywordRes();
        mockResponse.setDocuments(List.of(mockDocument));

        when(restTemplate.exchange(
                any(URI.class),
                any(),
                any(HttpEntity.class),
                eq(KakaoKeywordRes.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // When
        List<LocationDto> result = locationService.searchAddress(keyword);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getAddress()).isEqualTo("서울 강남구 테헤란로 152");
        assertThat(result.get(0).getJibunAddress()).isEqualTo("서울 강남구 강남동 123");
        assertThat(result.get(0).getLatitude()).isEqualTo(37.4979);
        assertThat(result.get(0).getLongitude()).isEqualTo(127.0276);
    }

    @Test
    @DisplayName("자동완성 주소 검색 - 도로명 주소 없을 때")
    void testSearchAddress_NoRoadAddress() {
        // Given
        String keyword = "강남";
        KakaoKeywordRes.Document docNoRoad = new KakaoKeywordRes.Document();
        docNoRoad.setAddressName("서울 강남구 강남동 123");
        docNoRoad.setRoadAddressName("");  // 도로명 없음
        docNoRoad.setX(127.0276);
        docNoRoad.setY(37.4979);

        KakaoKeywordRes mockResponse = new KakaoKeywordRes();
        mockResponse.setDocuments(List.of(docNoRoad));

        when(restTemplate.exchange(
                any(URI.class),
                any(),
                any(HttpEntity.class),
                eq(KakaoKeywordRes.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // When
        List<LocationDto> result = locationService.searchAddress(keyword);

        // Then
        assertThat(result).isNotEmpty();
        // 도로명이 없으면 지번주소 사용
        assertThat(result.get(0).getAddress()).isEqualTo("서울 강남구 강남동 123");



    }

    @Test
    @DisplayName("자동완성 최대 10개까지만 반환")
    void testSearchAddress_MaxTenResults() {
        // Given
        List<KakaoKeywordRes.Document> documents = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            KakaoKeywordRes.Document doc = new KakaoKeywordRes.Document();
            doc.setAddressName("서울 강남구 강남동 " + i);
            doc.setRoadAddressName("서울 강남구 테헤란로 " + i);
            doc.setX(127.0276 + i);
            doc.setY(37.4979 + i);
            documents.add(doc);
        }

        KakaoKeywordRes mockResponse = new KakaoKeywordRes();
        mockResponse.setDocuments(documents);

        when(restTemplate.exchange(
                any(URI.class),
                any(),
                any(HttpEntity.class),
                eq(KakaoKeywordRes.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // When
        List<LocationDto> result = locationService.searchAddress("강남");

        // Then
        assertThat(result).hasSize(10);  // 최대 10개
    }

    @Test
    @DisplayName("주소 검색창에 아무것도 입력을 안했을 때")
    void searchAddress_NullKeyword() {
        assertThrows(ApiException.class,
                () -> locationService.searchAddress(null));
    }

    @Test
    @DisplayName("동일지점 위치 계산 테스트")
    void calculateDistance_SamePoint() {

        double lat = 37.5665;
        double lng = 126.9780;

        double distance =
                locationService.calculateDistance(lat, lng, lat, lng);

        assertEquals(0, distance);
    }

    @Test
    @DisplayName("서울에서 부산까지 거리계산하기")
    void calculateDistance_SeoulToBusan() {

        double seoulLat = 37.5665;
        double seoulLng = 126.9780;

        double busanLat = 35.1796;
        double busanLng = 129.0756;

        double distance =
                locationService.calculateDistance(
                        seoulLat, seoulLng,
                        busanLat, busanLng
                );

        assertTrue(distance > 320 && distance < 330);//서울-부산 약 325km
    }

}
