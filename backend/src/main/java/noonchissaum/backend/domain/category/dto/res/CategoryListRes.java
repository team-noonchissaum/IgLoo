package noonchissaum.backend.domain.category.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.category.entity.Category;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryListRes {

    private Long id;
    private String name;
    private Long parentId;


    public static CategoryListRes from(Category c) {
        return CategoryListRes.builder()
                .id(c.getId())
                .name(c.getName())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .build();
    }
}

