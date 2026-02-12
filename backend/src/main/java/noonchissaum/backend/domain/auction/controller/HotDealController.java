package noonchissaum.backend.domain.auction.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.dto.req.AuctionRegisterReq;
import noonchissaum.backend.domain.auction.dto.res.AuctionRes;
import noonchissaum.backend.domain.auction.service.AuctionService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/hot-deals")
public class HotDealController {
    private final AuctionService auctionService;

    @PostMapping
    public ApiResponse<Long> create(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody AuctionRegisterReq req
    ) {
        Long auctionId = auctionService.registerHotDeal(userPrincipal.getUserId(), req);
        return ApiResponse.success("핫딜 경매 생성", auctionId);
    }



}
