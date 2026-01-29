package noonchissaum.backend.domain.item.repository;

import noonchissaum.backend.domain.item.entity.Item;
import noonchissaum.backend.domain.item.entity.Wish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishRepository extends JpaRepository<Wish, Long> {
    Optional<Wish> findByUserIdAndItemId(Long userId, Long itemId);

    @Query("select w.item from Wish w "+
            "join w.item i " +
            "join fetch i.seller " +
            "where w.user.id = :userId " +
            "order by w.createdAt desc ")
    List<Item> findItemsByUserId(Long userId);
    boolean existsByUserIdAndItemId(Long userId, Long itemId);

    @Query("""
    select i.id
    from Wish w
    join w.item i
    where w.user.id = :userId
      and i.id in :itemIds
""")
    List<Long> findWishedItemIds(
            @Param("userId") Long userId,
            @Param("itemIds") List<Long> itemIds
    );

}
