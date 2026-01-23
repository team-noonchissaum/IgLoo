package noonchissaum.backend.domain.item.controller;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.item.dto.WishItemRes;
import noonchissaum.backend.domain.item.dto.WishToggleRes;
import noonchissaum.backend.domain.item.service.WishService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/item")
public class WishController {
    private final WishService wishService;

    @PostMapping("/{itemId}/wish")
    public WishToggleRes wishToggleRes(@PathVariable Long itemId){
        Long userId = 1L;
        boolean wished = wishService.wishToggle(userId,itemId);
        return new WishToggleRes(wished);
    }

    @GetMapping("/wish")
    public List<WishItemRes> getMyWishlist(){
        Long userId = 1L;
        return wishService.getMyWishedItems(userId).stream()
                .map(WishItemRes::from)
                .toList();
    }
}
