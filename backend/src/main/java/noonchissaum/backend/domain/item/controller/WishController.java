package noonchissaum.backend.domain.item.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.item.dto.WishItemRes;
import noonchissaum.backend.domain.item.dto.WishToggleRes;
import noonchissaum.backend.domain.item.service.WishService;
import noonchissaum.backend.global.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<WishToggleRes>> wishToggleRes(@PathVariable Long itemId){
        Long userId = 1L;
        boolean wished = wishService.wishToggle(userId,itemId);
        return ResponseEntity.ok(
                ApiResponse.success("찜 상태가 변경되었습니다.", WishToggleRes.of(wished))
        );
    }

    @Transactional(readOnly = true)
    @GetMapping("/wish")
    public ResponseEntity<ApiResponse<List<WishItemRes>>> getMyWishlist(Reader reader){
        Long userId = 1L;
        List<WishItemRes> result = wishService.getMyWishedItems(userId).stream()
                .map(WishItemRes::from)
                .toList();
        return ResponseEntity.ok(
                ApiResponse.success("찜한 상품 목록 조회 성공", result)
        );
    }
}
