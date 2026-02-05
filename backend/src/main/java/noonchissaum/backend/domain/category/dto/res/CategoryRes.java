package noonchissaum.backend.domain.category.dto.res;

import noonchissaum.backend.domain.category.entity.Category;

public record CategoryRes(
        Long id,
        String name,
        Long parentId
) { public static CategoryRes from(Category category) {
    return new CategoryRes(
            category.getId(),
            category.getName(),
            category.getParent().getId()
    );
}
}
