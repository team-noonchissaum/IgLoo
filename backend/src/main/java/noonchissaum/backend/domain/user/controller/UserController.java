package noonchissaum.backend.domain.user.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import noonchissaum.backend.domain.user.dto.request.CreateCategorySubscriptionReq;
import noonchissaum.backend.domain.user.dto.request.ProfileUpdateUserReq;
import noonchissaum.backend.domain.user.dto.response.*;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import jakarta.validation.Valid;
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
     * 내 관심 카테고리 조회
     */
    @GetMapping("/me/category-subscriptions")
    public ResponseEntity<ApiResponse<CategorySubscriptionRes>> getMyCategorySubscriptions(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CategorySubscriptionRes result = userService.getMyCategorySubscriptions(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("조회 성공", result));
    }

    /** 내 관심 카테고리 등록 */
    @PostMapping("/me/category-subscriptions")
    public ResponseEntity<ApiResponse<CategorySubscriptionRes>> addMyCategorySubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCategorySubscriptionReq req
    ) {
        CategorySubscriptionRes result =
                userService.addMyCategorySubscription(principal.getUserId(), req.getCategoryId());
        return ResponseEntity.ok(ApiResponse.success("관심 카테고리 등록 성공", result));
    }

    /** 내 관심 카테고리 해제 */
    @DeleteMapping("/me/category-subscriptions/{categoryId}")
    public ResponseEntity<ApiResponse<CategorySubscriptionRes>> removeMyCategorySubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long categoryId
    ) {
        CategorySubscriptionRes result =
                userService.removeMyCategorySubscription(principal.getUserId(), categoryId);
        return ResponseEntity.ok(ApiResponse.success("관심 카테고리 해제 성공", result));
    }
}

