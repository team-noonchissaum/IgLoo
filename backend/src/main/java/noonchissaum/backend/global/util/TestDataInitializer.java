package noonchissaum.backend.global.util;

import lombok.RequiredArgsConstructor;
import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.Bid;
import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.category.repository.CategoryRepository;
import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.repository.ItemRepository;
import noonchissaum.backend.domain.auction.repository.AuctionRepository;
import noonchissaum.backend.domain.auction.repository.BidRepository;
import noonchissaum.backend.domain.user.entity.User;
import noonchissaum.backend.domain.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    private final ItemRepository itemRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // 유저 2명 (판매자/입찰자A)
        User seller = ensureUser("test@email.com", "password123!");
        User bidderA = ensureUser("test@email.com", "password123!");

        // 카테고리 1개
        Category category = categoryRepository.findByName("test-category")
                .orElseGet(() -> categoryRepository.save(new Category("test-category", null)));

        // 옥션 3개 + 각 옥션마다 bidderA가 6/12/20개의 입찰 생성
        long suffix = System.currentTimeMillis();

        createAuctionWithBids(seller, bidderA, category, suffix, 1, 6, 10_000);
        createAuctionWithBids(seller, bidderA, category, suffix, 2, 12, 20_000);
        createAuctionWithBids(seller, bidderA, category, suffix, 3, 20, 30_000);
    }

    private void createAuctionWithBids(
            User seller,
            User bidderA,
            Category category,
            long suffix,
            int auctionNo,
            int bidCount,
            long startPrice
    ) {
        // Item (매번 유니크)
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("test-item-" + suffix + "-" + auctionNo)
                .description("test description " + suffix + "-" + auctionNo)
                .startPrice(BigDecimal.valueOf(startPrice))
                .build());

        // Auction
        Auction auction = auctionRepository.save(Auction.builder()
                .item(item)
                .startPrice(item.getStartPrice())
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(2))
                .build());

        // READY -> RUNNING
        auction.run();

        // Bid 생성: bidderA가 bidCount만큼 입찰
        // 가격은 시작가에서 조금씩 증가시키는 형태로 생성 (myMax/currentMax/count 테스트에 좋음)
        BigDecimal price = item.getStartPrice();
        for (int i = 1; i <= bidCount; i++) {
            price = price.add(BigDecimal.valueOf(1000)); // 1천원씩 증가 (원하면 조절)
            bidRepository.save(new Bid(
                    auction,
                    bidderA,
                    price,
                    "test-" + suffix + "-" + auctionNo + "-" + i + "-" + UUID.randomUUID()
            ));
        }

        // auction.current_*도 맞춰주고 싶으면 마지막 bid 기준으로 갱신
        auction.updateBid(bidderA, price);
    }

    private User ensureUser(String email, String nickname) {
        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email).orElseThrow();
        }
        String finalNickname = nickname;
        if (userRepository.existsByNickname(nickname)) {
            finalNickname = nickname + "-" + System.currentTimeMillis();
        }
        return userRepository.save(User.createLocalUser(email, finalNickname));
    }
}
