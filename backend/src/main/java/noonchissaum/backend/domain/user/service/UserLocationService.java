package noonchissaum.backend.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.user.dto.request.UserLocationUpdateReq;
import noonchissaum.backend.domain.user.dto.response.UserLocationUpdateRes;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.repository.UserRepository;
import noonchissaum.backend.global.dto.LocationDto;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.service.LocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 위치 관련 기능
 * 위치 설정/수정, 내 위치 조회
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLocationService {

    private final UserRepository userRepository;
    private final LocationService locationService;

    /**
     * 위치 설정 / 수정
     * 사용자가 입력한 주소를 좌표로 변환해서 저장
     * Item에는 위치 저장 X   User 위치를 참조
     * 유저 위치 변경하면 해당 유저의 모든 Item도 자동 반영
     */
    @Transactional
    public UserLocationUpdateRes updateLocation(Long userId, UserLocationUpdateReq req) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        // 주소를 좌표 변환
        // 내부에서 주소 검색 실패 시 키워드 검색으로 fallback
        LocationDto locationDto = locationService.getCoordinates(req.getAddress());

        // 주소에서 동 정보 추출
        String dong = extractDong(locationDto.getAddress(), locationDto.getJibunAddress());

        double[] normalized = normalizeCoordinates(locationDto.getLatitude(), locationDto.getLongitude(), req.getAddress());
        double latitude = normalized[0];
        double longitude = normalized[1];

        user.updateLocation(
                locationDto.getAddress(),
                dong,
                latitude,
                longitude
        );

        return UserLocationUpdateRes.builder()
                .latitude(latitude)
                .longitude(longitude)
                .address(locationDto.getAddress())
                .jibunAddress(locationDto.getJibunAddress())
                .dong(dong)
                .build();
    }

    /**
     * 내 위치 조회 (마이페이지용)
     */
    public UserLocationUpdateRes getMyLocation(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        return UserLocationUpdateRes.builder()
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .address(user.getAddress())
                .dong(user.getDong())
                .build();
    }

    /**
     * 주소에서 동 정보 추출
     */
    private String extractDong(String roadAddress, String jibunAddress) {

        String dong = extractDongFromAddress(roadAddress);
        if (dong != null) {
            return dong;
        }

        dong = extractDongFromAddress(jibunAddress);
        return dong;
    }

    /**
     * 단일 주소에서 동 추출
     * 정규식: "~동", "~읍", "~면" 패턴
     */
    private String extractDongFromAddress(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }

        // 패턴 컴파일
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([가-힣]+[동읍면])");
        // 매처 생성 (문자열에 패턴 적용)
        java.util.regex.Matcher matcher = pattern.matcher(address);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private double[] normalizeCoordinates(Double latitude, Double longitude, String address) {
        if (latitude == null || longitude == null) {
            throw new ApiException(ErrorCode.INVALID_LOCATION_PARAMS);
        }

        boolean latOk = latitude >= -90 && latitude <= 90;
        boolean lonOk = longitude >= -180 && longitude <= 180;
        if (latOk && lonOk) {
            return new double[]{latitude, longitude};
        }

        boolean swappedLatOk = longitude >= -90 && longitude <= 90;
        boolean swappedLonOk = latitude >= -180 && latitude <= 180;
        if (swappedLatOk && swappedLonOk) {
            log.warn("위경도 값이 뒤바뀌어 있어 자동 교정합니다. address={}, lat={}, lon={}", address, latitude, longitude);
            return new double[]{longitude, latitude};
        }

        log.warn("유효하지 않은 위경도 값입니다. address={}, lat={}, lon={}", address, latitude, longitude);
        throw new ApiException(ErrorCode.INVALID_LOCATION_PARAMS);
    }
}
