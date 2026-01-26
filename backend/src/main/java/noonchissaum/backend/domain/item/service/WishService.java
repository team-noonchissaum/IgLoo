package noonchissaum.backend.domain.item.service;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.entity.Wish;
import noonchissaum.backend.domain.item.repository.ItemRepository;
import noonchissaum.backend.domain.item.repository.WishRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                    User user = userService.getUserId(userId);
                    Item item = itemService.getActiveById(itemId);

                    try {
                        wishRepository.save(Wish.of(user, item));
                        int updated = itemRepository.incrementWishCountIfActive(itemId);
                        if (updated == 0){
                            throw new IllegalStateException("삭제된 상품은 찜할 수 없습니다.");
                        }
                        return true;
                    } catch (DataIntegrityViolationException e) {
                    return true;
                    }
                });

    }

    public List<Item> getMyWishedItems(Long userId) {
        return wishRepository.findItemsByUserId(userId);
    }


}
