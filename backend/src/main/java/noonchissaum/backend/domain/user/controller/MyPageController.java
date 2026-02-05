package noonchissaum.backend.domain.user.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.dto.res.AuctionRes;
import noonchissaum.backend.domain.auction.dto.res.MyBidAuctionRes;
import noonchissaum.backend.domain.auction.service.BidService;
import noonchissaum.backend.domain.user.service.MyPageService;
import noonchissaum.backend.domain.user.dto.response.MyPageRes;
import noonchissaum.backend.domain.user.dto.response.UserWalletRes;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final BidService bidService;

    /**마이페이지 조회
     * GET /api/mypage */
    @GetMapping
    public ResponseEntity<ApiResponse<MyPageRes>> getMyPage(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MyPageRes response = myPageService.getMyPage(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("마이페이지 조회 성공", response));
    }

    /**
     * 내가 등록한 경매 목록 조회
     * GET /api/mypage/auctions
     */
    @GetMapping("/auctions")
    public ResponseEntity<ApiResponse<Page<AuctionRes>>> getMyAuctions(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable
    ) {
        Page<AuctionRes> response = myPageService.getMyAuctions(principal.getUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("내 등록 경매 조회 성공", response));
    }

}
