package noonchissaum.backend.domain.item.repository;

import noonchissaum.backend.domain.category.entity.Category;
import noonchissaum.backend.domain.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByIdAndStatusTrue(Long itemid);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Item i " +
            "set i.wishCount = coalesce(i.wishCount, 0) + 1 " +
            "where i.id = :itemId and i.status = true ")
    int incrementWishCountIfActive(Long itemId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Item i " +
            "set i.wishCount = case\n" +
            "                  when coalesce(i.wishCount, 0) > 0 then coalesce(i.wishCount, 0) - 1\n" +
            "                  else 0\n" +
            "                  end\n" +
            "where i.id = :itemId ")
    int decrementWishCount(Long itemId);

    @EntityGraph(attributePaths = {"seller", "category"})
    List<Item>findByCategoryIn(List<Category> categories);


}
