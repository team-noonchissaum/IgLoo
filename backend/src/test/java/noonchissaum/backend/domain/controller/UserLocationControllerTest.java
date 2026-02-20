package noonchissaum.backend.domain.controller;

import noonchissaum.backend.domain.user.dto.request.UserLocationUpdateReq;
import noonchissaum.backend.domain.user.dto.response.UserLocationUpdateRes;
import noonchissaum.backend.domain.user.service.UserLocationService;
import noonchissaum.backend.global.dto.LocationDto;
import noonchissaum.backend.global.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UserLocationController 통합 테스트")
class UserLocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserLocationService userLocationService;

    private LocationService locationService;

    private LocationDto mockLocationDto;
    private UserLocationUpdateRes mockUpdateRes;

    @BeforeEach
    void setUp() {
        mockLocationDto = LocationDto.builder()
                .latitude(37.4979)
                .longitude(127.0276)
                .address("서울 강남구 테헤란로 152")
                .jibunAddress("서울 강남구 강남동 123")
                .dong(null)
                .build();

        mockUpdateRes = UserLocationUpdateRes.builder()
                .latitude(37.4979)
                .longitude(127.0276)
                .address("서울 강남구 테헤란로 152")
                .jibunAddress("서울 강남구 강남동 123")
                .dong("강남동")
                .build();
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("주소 검색 API 성공")
    void testSearchAddress_Success() throws Exception {
        // Given
        String keyword = "강남";
        List<LocationDto> mockResults = List.of(mockLocationDto);

        when(locationService.searchAddress(keyword))
                .thenReturn(mockResults);

        // When & Then
        mockMvc.perform(get("/api/users/location/search")
                        .param("keyword", keyword)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("주소 검색 결과"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("주소 검색 API - 검색어 부족")
    void testSearchAddress_ShortKeyword() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/location/search")
                        .param("keyword", "강")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("위치 저장 API 성공")
    void testUpdateLocation_Success() throws Exception {
        // Given
        String requestBody = "{\"address\": \"서울 강남구 테헤란로 152\"}";

        when(userLocationService.updateLocation(anyLong(), any(UserLocationUpdateReq.class)))
                .thenReturn(mockUpdateRes);

        // When & Then
        mockMvc.perform(put("/api/users/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("위치 저장 완료"))
                .andExpect(jsonPath("$.data.dong").value("강남동"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("내 위치 조회 API 성공")
    void testGetMyLocation_Success() throws Exception {
        // Given
        when(userLocationService.getMyLocation(anyLong()))
                .thenReturn(mockUpdateRes);

        // When & Then
        mockMvc.perform(get("/api/users/location")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("위치 조회 성공"))
                .andExpect(jsonPath("$.data.latitude").value(37.4979));
    }

    @Test
    @DisplayName("인증 없이 접근 불가")
    void testWithoutAuthentication() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/location"))
                .andExpect(status().isUnauthorized());
    }
}
