package noonchissaum.backend.domain.user.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.user.dto.request.UserLocationUpdateReq;
import noonchissaum.backend.domain.user.dto.response.UserLocationUpdateRes;
import noonchissaum.backend.domain.user.service.UserLocationService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.dto.LocationDto;
import noonchissaum.backend.global.security.UserPrincipal;
import noonchissaum.backend.global.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/users/location")
@RequiredArgsConstructor
public class UserLocationController {

    private final UserLocationService userLocationService;
    private final LocationService locationService;

    /**
     * 내 위치 조회 (마이페이지용)
     * GET /api/users/location
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserLocationUpdateRes>> getMyLocation(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        UserLocationUpdateRes res = userLocationService.getMyLocation(
                userPrincipal.getUserId());

        return ResponseEntity.ok(ApiResponse.success("위치 조회 성공", res));
    }

    @GetMapping("/search")
    public List<LocationDto> searchAddress(String keyword) {
        return locationService.searchAddress(keyword);  // ← 위임
    }

    /**
     * 위치 설정 / 수정
     */
    @PutMapping
    @Transactional
    public ResponseEntity<ApiResponse<UserLocationUpdateRes>> updateLocation(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserLocationUpdateReq req) {

        UserLocationUpdateRes res =
                userLocationService.updateLocation(principal.getUserId(), req);


        return ResponseEntity.ok(ApiResponse.success("위치 저장 완료", res));
    }
}
