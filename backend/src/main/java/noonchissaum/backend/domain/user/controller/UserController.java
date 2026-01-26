package noonchissaum.backend.domain.user.controller;


import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auth.dto.request.SignupReq;
import noonchissaum.backend.domain.user.dto.response.MyPageRes;
import noonchissaum.backend.domain.user.dto.response.OtherUserProfileRes;
import noonchissaum.backend.domain.user.dto.response.ProfileRes;
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

    /**본인 프로필 조회하기*/
    @GetMapping("/me")
    public ResponseEntity<ProfileRes> myProfile(
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(userService.getMyProfile(userId));
    }



    /**
     * 마이페이지 조회
     */
    @GetMapping("/me/mypage")
    public ResponseEntity<ApiResponse<MyPageRes>> getMyPage(@AuthenticationPrincipal Long userId) {
        MyPageRes response = userService.getMyPage(userId);
        return ResponseEntity.ok(ApiResponse.success("요청 성공", response));

    }

    /**
     * 다른 유저 프로필 조회
     */

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<OtherUserProfileRes>> getUserProfile(@PathVariable Long userId) {
        OtherUserProfileRes response = userService.getOtherUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("요청 성공", response));

    }

    /**
     * 회원 탈퇴
     */

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal Long userId) {
        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }
}

