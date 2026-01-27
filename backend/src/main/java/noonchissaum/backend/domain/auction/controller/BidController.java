package noonchissaum.backend.domain.auction.controller;

import jakarta.validation.Valid;
import noonchissaum.backend.domain.auction.dto.MyBidAuctionRes;
import noonchissaum.backend.domain.auction.dto.PlaceBidReq;
import noonchissaum.backend.domain.auction.dto.BidHistoryItemRes;
import noonchissaum.backend.domain.auction.service.BidService;
import noonchissaum.backend.global.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bid")
public class BidController {
    private final BidService bidService;

//    @GetMapping("/{auctionId}")
//    public ResponseEntity<ApiResponse<Page<BidHistoryItemRes>>> getBidHistory(
//            @PathVariable Long auctionId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size
//    ){
//        Pageable pageable = PageRequest.of(page,size);
//
//        Page<BidHistoryItemRes> bidHistory = bidService.getBidHistory(auctionId, pageable);
//
//        return ResponseEntity.ok(ApiResponse.success("조회 완료",bidHistory));
//
//    }
//
//    @GetMapping("/my")
//    public ResponseEntity<ApiResponse<Page<MyBidAuctionRes>>> getMyBidAuctions(
//            //@AuthenticationPrincipal CustomUserDetails userDetails,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size
//    ) {
//        Pageable pageable = PageRequest.of(page, size);
//
//        Page<MyBidAuctionRes> result =
//                bidService.getMyBidAuctions(userDetails.getUserId(), pageable);
//
//        String message = result.isEmpty()
//                ? "입찰 참여 내역이 존재하지 않습니다."
//                : "조회가 완료되었습니다.";
//
//        return ResponseEntity.ok(
//                ApiResponse.success(message, result)
//        );
//    }
//
//
//
//
//    @PostMapping()
//    public ResponseEntity<ApiResponse<Void>> placeBid(
//            //@AuthenticationPrincipal CustomUserDetails userDetails,
//            @Valid @RequestBody PlaceBidReq req
//    ){
//        bidService.placeBid(req.auctionId(), userDetails.getUserId(), req.bidAmount(), req.requestId());
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(ApiResponse.success("입찰 완료"));
//
//    }

}
