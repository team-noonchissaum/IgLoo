package noonchissaum.backend.domain.user.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.user.dto.request.UserLocationUpdateReq;
import noonchissaum.backend.domain.user.dto.response.UserLocationUpdateRes;
import noonchissaum.backend.domain.user.service.UserLocationService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users/location")
@RequiredArgsConstructor
public class UserLocationController {

    private final UserLocationService userLocationService;

    /**
     * 위치 설정/수정
     * PUT /api/users/location
     */
    @PutMapping
    public ResponseEntity<ApiResponse<UserLocationUpdateRes>> updateLocation(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UserLocationUpdateReq req) {

        UserLocationUpdateRes res = userLocationService.updateLocation(
                userPrincipal.getUserId(), req);

        return ResponseEntity.ok(ApiResponse.success("위치 설정 성공", res));
    }

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
}
