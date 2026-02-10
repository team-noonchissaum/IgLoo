package noonchissaum.backend.domain.user.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.user.dto.request.ProfileUpdateUserReq;
import noonchissaum.backend.domain.user.dto.request.UserLocationUpdateReq;
import noonchissaum.backend.domain.user.dto.response.*;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /** 본인 프로필 조회
     * GET /api/users/me
     */

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileRes>> myProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        ProfileRes response = userService.getMyProfile(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("요청 성공", response));
    }

    /**
     * 다른 유저 프로필 조회
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<OtherUserProfileRes>> getUserProfile(@PathVariable Long userId) {
        OtherUserProfileRes response = userService.getOtherUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("요청 성공", response));
    }
    /**
     * 프로필 수정
     * PATCH /api/users/me
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<ProfileUpdateUserRes>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ProfileUpdateUserReq request) {
        ProfileUpdateUserRes response = userService.updateProfile(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("수정 성공", response));
    }
    /**
     * 탈퇴 시도 (첫 클릭)
     */
    @PostMapping("/me/delete-attempt")
    public ResponseEntity<ApiResponse<UserDeleteAttemptRes>> attemptDelete(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserDeleteAttemptRes result = userService.attemptDelete(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("탈퇴 시도 완료", result));
    }

    /**
     * 강제 탈퇴 (두 번째 클릭 후 확인)
     */
    @DeleteMapping("/me/force")
    public ResponseEntity<ApiResponse<Void>> forceDelete(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userService.userDelete(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴 완료"));
    }

    /**
     * 사용자 위치 업데이트
     * POST /api/users/me/location
     */
    @PostMapping("/me/location")
    public ResponseEntity<ApiResponse<Void>> updateMyLocation(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserLocationUpdateReq request
    ) {
        User user = userService.getById(principal.getUserId());

        userService.updateUserLocation(user, request.getAddress());

        return ResponseEntity.ok(
                ApiResponse.success("사용자 위치가 업데이트되었습니다.")
        );
    }
}

