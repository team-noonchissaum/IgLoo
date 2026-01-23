package noonchissaum.backend.domain.item.repository;

import noonchissaum.backend.domain.item.entity.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemImageRepository extends JpaRepository<ItemImage, Long> {
}
