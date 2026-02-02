package noonchissaum.backend.domain.item.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.item.dto.WishItemRes;
import noonchissaum.backend.domain.item.dto.WishToggleRes;
import noonchissaum.backend.domain.item.service.WishService;
import noonchissaum.backend.global.dto.ApiResponse;
import noonchissaum.backend.global.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.Reader;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/item")
public class WishController {
    private final WishService wishService;

    @PostMapping("/{itemId}/wish")
    public ResponseEntity<ApiResponse<WishToggleRes>> wishToggleRes(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal userPrincipal){
        Long userId = userPrincipal.getUserId();
        boolean wished = wishService.wishToggle(userId,itemId);
        return ResponseEntity.ok(
                ApiResponse.success("찜 상태가 변경되었습니다.", WishToggleRes.of(wished))
        );
    }

    @GetMapping("/wish")
    public ResponseEntity<ApiResponse<List<WishItemRes>>> getMyWishlist(
            @AuthenticationPrincipal UserPrincipal userPrincipal){
        Long userId = userPrincipal.getUserId();
        List<WishItemRes> result = wishService.getMyWishedItems(userId);
        return ResponseEntity.ok(
                ApiResponse.success("찜한 상품 목록 조회 성공", result)
        );
    }
}
