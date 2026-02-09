package noonchissaum.backend.domain.category.dto.req;

public record AddCategoryReq(
        Long parentId,
        String name
) {
}
