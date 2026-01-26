package noonchissaum.backend.domain.user.controller;


import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.user.dto.request.ProfileUpdateUserReq;
import noonchissaum.backend.domain.user.dto.response.MyPageRes;
import noonchissaum.backend.domain.user.dto.response.OtherUserProfileRes;
import noonchissaum.backend.domain.user.dto.response.ProfileRes;
import noonchissaum.backend.domain.user.dto.response.ProfileUpdateUserRes;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.global.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
            @AuthenticationPrincipal Long userId
    ) {
        ProfileRes response = userService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("요청 성공", response));
    }

    /**
     * 마이페이지 조회
     * GET /api/users/me/mypage
     */
    @GetMapping("/me/mypage")
    public ResponseEntity<ApiResponse<MyPageRes>> getMyPage(@AuthenticationPrincipal Long userId) {
        MyPageRes response = userService.getMyPage(userId);
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
    public ResponseEntity<ApiResponse<ProfileUpdateUserRes>> updateProfile(@AuthenticationPrincipal Long userId,
            @RequestBody ProfileUpdateUserReq request) {
        ProfileUpdateUserRes response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("수정 성공", response));
    }

    /**
     * 회원 탈퇴
     * DELETE /api/users/me
     */

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal Long userId) {
        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }
}

