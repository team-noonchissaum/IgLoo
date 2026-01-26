package noonchissaum.backend.domain.user.repository;

import noonchissaum.backend.domain.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}