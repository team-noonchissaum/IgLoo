package noonchissaum.backend.domain.item.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.item.dto.WishItemRes;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.entity.Wish;
import noonchissaum.backend.domain.item.repository.ItemRepository;
import noonchissaum.backend.domain.item.repository.WishRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import noonchissaum.backend.global.exception.ApiException;
import noonchissaum.backend.global.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WishService {

    private final WishRepository wishRepository;
    private final ItemService itemService;
    private final ItemRepository itemRepository;
    private final UserService userService;

    /**
     *찜하기 기능
     */
    @Transactional
    public boolean wishToggle(Long userId, Long itemId){
        return wishRepository.findByUserIdAndItemId(userId, itemId)
                .map(existing -> {
                    wishRepository.delete(existing);
                    itemRepository.decrementWishCount(itemId);
                    return false;
                })
                .orElseGet(() -> {
                    User user = userService.getUserByUserId(userId);
                    Item item = itemService.getActiveById(itemId);

                    try {
                        wishRepository.save(Wish.of(user, item));
                        int updated = itemRepository.incrementWishCountIfActive(itemId);
                        if (updated == 0){
                            throw new ApiException(ErrorCode.ITEM_ALREADY_BLOCKED);
                        }
                        return true;
                    } catch (DataIntegrityViolationException e) {
                    return true;
                    }
                });
    }

    @Transactional(readOnly = true)
    public boolean isWished(Long userId, Long itemId){
        return wishRepository.existsByUserIdAndItemId(userId, itemId);
    }

    // 경매 목록에서 찜 부분 보이게 하는 메서드
    @Transactional(readOnly = true)
    public Set<Long> getWishedItemIds(Long userId, List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(wishRepository.findWishedItemIds(userId, itemIds));
    }

    @Transactional(readOnly = true)
    public List<WishItemRes> getMyWishedItems(Long userId) {
        List<Item> items = wishRepository.findItemsByUserId(userId);

        return items.stream()
                .map(WishItemRes::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Item> getWishedItemsByUser(Long userId) {
        return wishRepository.findItemsByUserId(userId);
    }
}
