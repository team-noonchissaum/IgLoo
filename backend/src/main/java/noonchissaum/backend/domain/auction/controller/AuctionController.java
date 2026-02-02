package noonchissaum.backend.domain.auction.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.dto.req.AuctionRegisterReq;
import noonchissaum.backend.domain.auction.dto.res.AuctionListRes;
import noonchissaum.backend.domain.auction.dto.res.AuctionRes;
import noonchissaum.backend.domain.auction.entity.AuctionSortType;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import noonchissaum.backend.domain.auction.service.AuctionService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 경매 관련 API를 제공하는 컨트롤러입니다.
 * 현재 인증 시스템 연동 전까지 'X-User-Id' 헤더를 통해 임시로 사용자 식별 정보를 전달받습니다.
 */
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    /**
     * 새로운 경매 물품을 등록합니다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> registerAuction(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody AuctionRegisterReq request) {
        Long auctionId = auctionService.registerAuction(userPrincipal.getUserId(), request);
        return ResponseEntity.created(URI.create("/api/auctions/" + auctionId))
                .body(new ApiResponse<>("Auction registered successfully", auctionId));
    }

    /**
     * 경매 목록을 페이징하여 조회합니다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuctionRes>>> getAuctions(
            @PageableDefault(size = 10, sort = "startAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) AuctionStatus status,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal != null ? userPrincipal.getUserId() : null;
        Page<AuctionRes> auctions = auctionService.getAuctionList(userId, pageable, status);
        return ResponseEntity.ok(new ApiResponse<>("Auction list retrieved", auctions));
    }

    /**
     * 특정 경매의 상세 정보를 조회합니다.
     */
    @GetMapping("/{auctionId}")
    public ResponseEntity<ApiResponse<AuctionRes>> getAuctionDetail(
            @PathVariable Long auctionId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal != null ? userPrincipal.getUserId() : null;
        AuctionRes response = auctionService.getAuctionDetail(userId, auctionId);
        return ResponseEntity.ok(new ApiResponse<>("Auction detail retrieved", response));
    }

    /**
     * 등록된 경매를 취소합니다.
     */
    @DeleteMapping("/{auctionId}")
    public ResponseEntity<ApiResponse<Void>> cancelAuction(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long auctionId) {
        Long userId = userPrincipal.getUserId();
        auctionService.cancelAuction(userId, auctionId);
        return ResponseEntity.ok(new ApiResponse<>("Auction canceled successfully", null));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<AuctionListRes>>> searchAuctions(
            @RequestParam(required = false) AuctionStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "LATEST") AuctionSortType sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal != null ? userPrincipal.getUserId() : null;
        Page<AuctionListRes> result = auctionService.searchAuctionList(
                userId, status, categoryId, keyword, sort, page, size
        );

        return ResponseEntity.ok(ApiResponse.success("Auction search retrieved", result));
    }
}
