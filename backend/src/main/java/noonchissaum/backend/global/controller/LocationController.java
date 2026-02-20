package noonchissaum.backend.global.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.dto.res.AuctionRes;
import noonchissaum.backend.domain.auction.service.AuctionService;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Arrays;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final AuctionService auctionService;
    private final UserService userService;

    /**
     * 사용자 위치 기준 주변 경매 검색->인증받은 유저
     * GET /api/location/auctions/nearby?userId=3&radiusKm=10&page=0&size=20
     */
    @GetMapping("/auctions/nearby")
    public ResponseEntity<ApiResponse<Page<AuctionRes>>> searchNearbyAuctions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Double radiusKm,
            Pageable pageable) {
        //1. 인증
        if (principal == null) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED);
        }
        //2. 반경 유효성
        if (!isValidRadius(radiusKm)) {
            throw new ApiException(ErrorCode.INVALID_RADIUS);
        }

        // 3. 사용자 위치 확인
        User user = userService.getUserByUserId(principal.getUserId());
        if (user.getLatitude() == null) {
            throw new ApiException(ErrorCode.USER_LOCATION_NOT_SET);
        }

        Page<AuctionRes> auctions = auctionService.searchAuctionsByUserLocation(
                principal.getUserId(),
                radiusKm,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success("주변 경매 검색 성공", auctions));
    }
    private boolean isValidRadius(Double radiusKm) {
        return Arrays.asList(1D, 3D, 7D, 10D, 20D, 50D).contains(radiusKm);
    }
}