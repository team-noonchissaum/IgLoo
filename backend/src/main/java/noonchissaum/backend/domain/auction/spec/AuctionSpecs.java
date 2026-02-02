package noonchissaum.backend.domain.auction.spec;

import noonchissaum.backend.domain.auction.entity.Auction;
import noonchissaum.backend.domain.auction.entity.AuctionStatus;
import org.springframework.data.jpa.domain.Specification;

public class AuctionSpecs {
    public static Specification<Auction> filter(
            AuctionStatus status,
            Long categoryId,
            String keyword
    ) {
        return (root, query, cb) -> {
            query.distinct(true);

            var predicate = cb.conjunction();

            // 1) Auction.status
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            // 2) join Auction -> Item
            var itemJoin = root.join("item");

            // 삭제된 상품 제외
             predicate = cb.and(predicate, cb.isTrue(itemJoin.get("status")));

            // 3) category filter
            if (categoryId != null) {
                var categoryJoin = itemJoin.join("category");
                predicate = cb.and(predicate, cb.equal(categoryJoin.get("id"), categoryId));
            }

            // 4) keyword filter (Item.title contains ignore-case)
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                predicate = cb.and(predicate, cb.like(cb.lower(itemJoin.get("title")), like));
            }

            return predicate;
        };
    }
}
