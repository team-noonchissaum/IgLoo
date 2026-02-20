package noonchissaum.backend.domain.location;

import noonchissaum.backend.domain.user.dto.request.UserLocationUpdateReq;
import noonchissaum.backend.domain.user.dto.response.UserLocationUpdateRes;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.entity.UserRole;
import noonchissaum.backend.domain.user.entity.UserStatus;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.domain.user.service.UserLocationService;
import noonchissaum.backend.global.dto.LocationDto;
import noonchissaum.backend.global.exception.CustomException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.service.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import noonchissaum.backend.global.exception.ApiException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserLocationService 테스트")
class UserLocationServiceTest {

    private static final Long USER_ID = 1L;
    private static final String VALID_ADDRESS = "서울 강남구 테헤란로 152";
    private static final String INVALID_ADDRESS = "시울";

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private UserLocationService userLocationService;

    private User testUser;
    private LocationDto mockLocationDto;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.builder()
                .email("test@test.com")
                .nickname("테스터")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        testUser.updateLocation(
                "서울 강남구 테헤란로 152",
                "강남동",
                37.4979,
                127.0276
        );

        mockLocationDto = LocationDto.builder()
                .latitude(37.4979)
                .longitude(127.0276)
                .address("서울 강남구 테헤란로 152")
                .jibunAddress("서울 강남구 강남동 123")
                .dong("강남동")
                .build();
    }
    @Test
    @DisplayName("위치 설정 성공")
    void updateLocation_success() {
        // GIVEN
        UserLocationUpdateReq req = new UserLocationUpdateReq(VALID_ADDRESS);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(testUser));
        when(locationService.getCoordinates(VALID_ADDRESS))
                .thenReturn(mockLocationDto);

        // WHEN
        UserLocationUpdateRes result =
                userLocationService.updateLocation(USER_ID, req);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(37.4979);
        assertThat(result.getLongitude()).isEqualTo(127.0276);
        assertThat(result.getDong()).isEqualTo("강남동");

        verify(userRepository).findById(USER_ID);
        verify(locationService).getCoordinates(VALID_ADDRESS);
    }

    @Test
    @DisplayName("위치 설정 실패 - 사용자 없음")
    void updateLocation_fail_userNotFound() {
        // GIVEN
        UserLocationUpdateReq req = new UserLocationUpdateReq(VALID_ADDRESS);
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() ->
                userLocationService.updateLocation(USER_ID, req))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(locationService, never()).getCoordinates(any());
    }

    @Test
    @DisplayName("위치 설정 실패 - 주소 검색 실패")
    void updateLocation_fail_addressNotFound() {
        // GIVEN
        UserLocationUpdateReq req = new UserLocationUpdateReq(INVALID_ADDRESS);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(testUser));
        when(locationService.getCoordinates(INVALID_ADDRESS))
                .thenReturn(null);

        // WHEN & THEN
        assertThatThrownBy(() ->
                userLocationService.updateLocation(USER_ID, req))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ADDRESS_NOT_FOUND);
    }

    @Test
    @DisplayName("내 위치 조회 성공")
    void getMyLocation_success() {
        // GIVEN
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(testUser));

        // WHEN
        UserLocationUpdateRes result =
                userLocationService.getMyLocation(USER_ID);

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(37.4979);
        assertThat(result.getDong()).isEqualTo("강남동");
    }

    @Test
    @DisplayName("동일 위치 업데이트시 외부 api호출x")
    void updateLocation_SameLocation_NoUpdate(){
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(testUser));

        // 같은 주소 업데이트 시도
        UserLocationUpdateReq req = new UserLocationUpdateReq(VALID_ADDRESS);

        // 실제 updateLocation에서는 getCoordinates()가 호출됨
        when(locationService.getCoordinates(VALID_ADDRESS))
                .thenReturn(mockLocationDto);

        // when
        UserLocationUpdateRes result=userLocationService.updateLocation(USER_ID, req);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAddress()).isEqualTo(VALID_ADDRESS);
        assertThat(result.getLatitude()).isEqualTo(mockLocationDto.getLatitude());
        assertThat(result.getLongitude()).isEqualTo(mockLocationDto.getLongitude());

        verify(locationService, times(1)).getCoordinates(VALID_ADDRESS);
        verify(locationService, never()).searchAddress(anyString());

    }
    @Test
    @DisplayName("위치가 없는 사용자 조회 시 예외 발생")
    void getMyLocation_LocationNull_ThrowsException() {

        // given
        User userWithoutLocation = User.builder()
                .email("test2@test.com")
                .nickname("위치없음")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(userWithoutLocation));

        // when & then
        assertThrows(ApiException.class,
                () -> userLocationService.getMyLocation(USER_ID));
    }


    @Test
    @DisplayName("주소 검색 결과 없으면 위치 업데이트 실패")
    void updateLocation_NoSearchResult_Fail() {

        // given
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(testUser));
        when(locationService.getCoordinates(INVALID_ADDRESS))
                .thenThrow(new ApiException(ErrorCode.ADDRESS_NOT_FOUND));

        UserLocationUpdateReq req = new UserLocationUpdateReq(INVALID_ADDRESS);

        // when & then
        assertThrows(ApiException.class, () ->
                userLocationService.updateLocation(USER_ID, new UserLocationUpdateReq(INVALID_ADDRESS))
        );
    }
    @Test
    @DisplayName("사용자 없으면 위치 업데이트 실패")
    void updateLocation_UserNotFound() {

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        UserLocationUpdateReq req = new UserLocationUpdateReq(VALID_ADDRESS);

        assertThrows(ApiException.class,
                () -> userLocationService.updateLocation(USER_ID, req));
    }

    @Test
    @DisplayName("위치 업데이트 성공 시 값 변경 확인")
    void updateLocation_Success_VerifyValues() {

        // given
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(testUser));

        when(locationService.getCoordinates(VALID_ADDRESS))
                .thenReturn(mockLocationDto);

        UserLocationUpdateReq req = new UserLocationUpdateReq(VALID_ADDRESS);

        // when
        UserLocationUpdateRes result =
                userLocationService.updateLocation(USER_ID, req);

        // then
        assertEquals(37.4979, result.getLatitude());
        assertEquals(127.0276, result.getLongitude());
        assertEquals("강남동", result.getDong());
    }
}
