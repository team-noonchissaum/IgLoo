package noonchissaum.backend.domain.mypage.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.mypage.service.MyPageService;
import noonchissaum.backend.domain.user.dto.response.MyPageRes;
import noonchissaum.backend.domain.user.dto.response.UserWalletRes;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    /**마이페이지 조회
     * GET /api/mypage */
    @GetMapping
    public ResponseEntity<ApiResponse<MyPageRes>> getMyPage(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MyPageRes response = myPageService.getMyPage(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("마이페이지 조회 성공", response));
    }

    /**지갑 정보 조회
     * GET /api/mypage/wallet*/
    @GetMapping("/wallet")
    public ResponseEntity<ApiResponse<UserWalletRes>> getWallet(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserWalletRes response = myPageService.getWallet(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("지갑 조회 성공", response));
    }
}
